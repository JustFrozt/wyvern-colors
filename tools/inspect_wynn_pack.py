#!/usr/bin/env python3
"""Read selected cached-pack JSON without trusting advertised uncompressed sizes.

Read-only diagnostics: no pack extraction, rewriting, or game/config changes.
Nonstandard ZIP metadata is reported; successful JSON parsing is not an integrity check.
"""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import argparse
import hashlib
import json
import struct
import zlib


JSON_LIMIT = 8 * 1024 * 1024
INPUT_LIMIT = 64 * 1024 * 1024
TARGETS = {2790, 2791, 2797, 2840, 2844, 2845, 2851}
DEFAULT_PACK = Path("minecraft/downloads/801e6a56-722d-300a-9630-66246d0e6c64/"
                    "03ff36a54f274ee967b3f5e9fba0f3627c0611a0")


def read_json_bytes(pack, name, report=print, limit=JSON_LIMIT):
    """Bound real output and input; raw DEFLATE's EOF supplies its actual boundary."""
    info = pack.getinfo(name)
    size = Path(pack.filename).stat().st_size
    with open(pack.filename, "rb") as stream:
        if not 0 <= info.header_offset <= size - 30:
            raise ValueError("Local header offset is outside this file")
        stream.seek(info.header_offset)
        header = struct.unpack("<4s5H3I2H", stream.read(30))
        if header[0] != b"PK\x03\x04":
            raise ValueError("Invalid local ZIP header signature")
        flags, method, local_crc, local_compressed, local_size = (
            header[2], header[3], header[6], header[7], header[8])
        local_name = stream.read(header[9])
        local_extra = stream.read(header[10])
        if len(local_name) != header[9] or len(local_extra) != header[10]:
            raise ValueError("Truncated local ZIP header")
        if flags & 1 or info.flag_bits & 1:
            raise ValueError("Encrypted entry is not supported")
        report(f"ENTRY {name}: central size={info.file_size}, compressed={info.compress_size}, "
               f"method={info.compress_type}; local size={local_size}, "
               f"compressed={local_compressed}, method={method}, filename_bytes={len(local_name)}")
        if method != info.compress_type:
            report("  NOTE: compression methods differ; using central method for bounded diagnostics only")
            method = info.compress_type
        if local_name.decode("utf-8" if flags & 0x800 else "cp437", errors="replace") != info.orig_filename:
            report("  NOTE: local and central names differ; using central name for diagnostics only")
        start = stream.tell()
        if method == 8:
            decoder = zlib.decompressobj(-15)
            data = bytearray()
            consumed = 0
            while not decoder.eof:
                chunk = stream.read(min(65536, INPUT_LIMIT - consumed))
                if not chunk:
                    raise ValueError("DEFLATE stream is truncated or exceeds the 64 MiB input limit")
                consumed += len(chunk)
                data.extend(decoder.decompress(chunk, limit + 1 - len(data)))
                if len(data) > limit:
                    raise ValueError(f"Actual decompressed JSON exceeds {limit} bytes")
                # A full output allowance with unfinished input would exceed our cap.
                if decoder.unconsumed_tail:
                    raise ValueError(f"Actual decompressed JSON exceeds {limit} bytes")
            compressed = consumed - len(decoder.unused_data)
        elif method == 0:
            # Stored data has no self-delimiting stream. Require a plausible length.
            following = [entry.header_offset for entry in pack.infolist()
                         if entry.header_offset > info.header_offset]
            boundary = min(following + [pack.start_dir, size])
            length = info.compress_size
            if not 0 <= length <= min(limit, boundary - start):
                raise ValueError("Stored entry has no trustworthy bounded length; header details needed")
            data = bytearray(stream.read(length))
            if len(data) != length:
                raise ValueError("Stored entry is truncated")
            compressed = length
        else:
            raise ValueError(f"Unsupported compression method {method}; header details needed")
    actual_crc = zlib.crc32(data) & 0xFFFFFFFF
    report(f"  ACTUAL: JSON bytes={len(data)}, compressed bytes={compressed}, CRC32={actual_crc:08x}")
    if len(data) != info.file_size or compressed != info.compress_size:
        report("  NOTE: advertised lengths differ from the bounded payload")
    if actual_crc != info.CRC:
        report(f"  WARNING: central CRC32 differs ({info.CRC:08x}); payload is diagnostic only")
    if not flags & 8 and local_crc != actual_crc:
        report(f"  WARNING: local CRC32 differs ({local_crc:08x}); payload is diagnostic only")
    return bytes(data)


def objects(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from objects(child)
    elif isinstance(value, list):
        for child in value:
            yield from objects(child)


def selected(entry):
    predicate = entry.get("predicate")
    values = (entry.get("threshold"), predicate.get("custom_model_data")
              if isinstance(predicate, dict) else None)
    return any(type(value) in (int, float) and value in TARGETS for value in values)


def inspect(path):
    with path.open("rb") as stream:
        print("PACK:", path)
        print("SHA1:", hashlib.file_digest(stream, "sha1").hexdigest())
    with ZipFile(path) as pack:
        names = set(pack.namelist())
        if "pack.mcmeta" in names:
            try:
                metadata = json.loads(read_json_bytes(pack, "pack.mcmeta"))
                print("PACK METADATA:", json.dumps(metadata, ensure_ascii=False)[:4000])
            except (OSError, ValueError, zlib.error, RecursionError) as error:
                print("METADATA READ ERROR:", type(error).__name__, str(error))
        selectors = sorted(name for name in names if name.endswith((
            "/items/potion.json", "/models/item/potion.json")))
        print("POTION SELECTORS:", selectors)
        pending = []
        for name in selectors:
            print("\nSELECTOR:", name)
            try:
                document = json.loads(read_json_bytes(pack, name))
                prefix = name.split("assets/", 1)[0]
                matches = [entry for entry in objects(document) if selected(entry)]
                print("Matching entries:", len(matches))
                for entry in matches:
                    text = json.dumps(entry, ensure_ascii=False)
                    print(text[:4000] + (" [output abbreviated]" if len(text) > 4000 else ""))
                    for node in objects(entry):
                        if isinstance(node.get("model"), str):
                            pending.append((prefix, node["model"]))
            except (OSError, ValueError, zlib.error, RecursionError) as error:
                print("SELECTOR READ ERROR:", type(error).__name__, str(error))
        seen = set()
        while pending and len(seen) < 40:
            prefix, reference = pending.pop(0)
            if (prefix, reference) in seen:
                continue
            seen.add((prefix, reference))
            namespace, separator, model = reference.partition(":")
            if not separator:
                namespace, model = "minecraft", reference
            base = f"assets/{namespace}/models/{model}.json"
            name = next((name for name in (prefix + base, base) if name in names), None)
            if name is None:
                print("MODEL NOT IN THIS PACK:", reference)
                continue
            try:
                document = json.loads(read_json_bytes(pack, name))
                print("MODEL:", name, json.dumps({key: document[key] for key in ("parent", "textures")
                                                   if key in document}, ensure_ascii=False))
                if isinstance(document.get("parent"), str):
                    pending.append((prefix, document["parent"]))
            except (OSError, ValueError, zlib.error, RecursionError) as error:
                print("MODEL READ ERROR:", name, type(error).__name__, str(error))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path, nargs="?", default=DEFAULT_PACK)
    args = parser.parse_args()
    try:
        inspect(args.pack)
    except (OSError, ValueError, BadZipFile, zlib.error, RecursionError) as error:
        print("PACK READ ERROR:", type(error).__name__, str(error))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
