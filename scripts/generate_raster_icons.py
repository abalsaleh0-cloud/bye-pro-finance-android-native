#!/usr/bin/env python3
"""Crop grayscale-alpha or RGBA sprite sheets into individual RGBA PNG resources (stdlib only)."""
from __future__ import annotations
import argparse
import struct
import zlib
from pathlib import Path

NAMES = [
    "ic_add", "ic_advance", "ic_ai", "ic_arrow_back", "ic_bill", "ic_bye_pro_finance",
    "ic_calculator", "ic_calendar", "ic_close", "ic_dashboard", "ic_debts", "ic_delete",
    "ic_edit", "ic_expense", "ic_expense_cleaning", "ic_expense_groceries",
    "ic_expense_health", "ic_expense_housing", "ic_expense_maintenance", "ic_expense_other",
    "ic_expense_personal", "ic_expense_transport", "ic_expense_utilities", "ic_home",
    "ic_home_bills", "ic_home_expenses", "ic_home_received", "ic_home_rent", "ic_more",
    "ic_payments", "ic_search", "ic_settings", "ic_transfer", "ic_work", "ic_work_advance",
    "ic_work_payment", "ic_work_remaining", "ic_work_salary", "ic_work_transfers",
]
PNG = b"\x89PNG\r\n\x1a\n"

def chunks(data: bytes):
    pos = 8
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        kind = data[pos+4:pos+8]
        payload = data[pos+8:pos+8+length]
        yield kind, payload
        pos += length + 12

def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)

def encode_png(path: Path, width: int, height: int, raw: bytes):
    def chunk(kind: bytes, payload: bytes):
        body = kind + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xffffffff)
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(PNG + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    data = Path(args.input).read_bytes()
    ihdr = next(payload for kind, payload in chunks(data) if kind == b"IHDR")
    width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", ihdr)
    if depth != 8 or color not in (4, 6) or (compression, filtering, interlace) != (0, 0, 0):
        raise SystemExit("Icon sheet must be non-interlaced 8-bit grayscale-alpha or RGBA PNG.")
    bytes_per_pixel = 4 if color == 6 else 2
    columns, cell = 8, 32
    rows = (len(NAMES) + columns - 1) // columns
    if width != columns * cell or height < rows * cell:
        raise SystemExit(f"Unexpected icon sheet dimensions: {width}x{height}")
    packed = b"".join(payload for kind, payload in chunks(data) if kind == b"IDAT")
    filtered = zlib.decompress(packed)
    stride = width * bytes_per_pixel
    decoded = bytearray(height * stride)
    src = 0
    for y in range(height):
        f = filtered[src]
        src += 1
        row = bytearray(filtered[src:src+stride])
        src += stride
        prev = decoded[(y-1)*stride:y*stride] if y else bytearray(stride)
        for i in range(stride):
            left = row[i-bytes_per_pixel] if i >= bytes_per_pixel else 0
            up = prev[i]
            upper_left = prev[i-bytes_per_pixel] if i >= bytes_per_pixel else 0
            if f == 1: row[i] = (row[i] + left) & 255
            elif f == 2: row[i] = (row[i] + up) & 255
            elif f == 3: row[i] = (row[i] + ((left + up) // 2)) & 255
            elif f == 4: row[i] = (row[i] + paeth(left, up, upper_left)) & 255
            elif f != 0: raise SystemExit(f"Unsupported PNG filter: {f}")
        decoded[y*stride:(y+1)*stride] = row
    output = Path(args.output) / "drawable-nodpi"
    output.mkdir(parents=True, exist_ok=True)
    for index, name in enumerate(NAMES):
        sx, sy = (index % columns) * cell, (index // columns) * cell
        tile = bytearray()
        for y in range(sy, sy+cell):
            tile.append(0)
            start = y*stride + sx*bytes_per_pixel
            if color == 6:
                tile.extend(decoded[start:start+cell*4])
            else:
                # PNG color type 4 stores grayscale + alpha. Expand each
                # visible grayscale sample to an RGBA pixel for Android.
                for x in range(cell):
                    gray, alpha = decoded[start+x*2:start+x*2+2]
                    tile.extend((gray, gray, gray, alpha))
        encode_png(output / (name + ".png"), cell, cell, bytes(tile))

if __name__ == "__main__":
    main()
