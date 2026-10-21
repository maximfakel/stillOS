#!/usr/bin/env python3
"""Copy the filled paths of a one-page vector PDF/AI logo into an SVG, geometry unchanged.

For brand files shipped as .ai/.pdf only (the YouTube white logo): reads the page content stream and turns
the PDF path operators (m l c v y h re, f/f*, q/Q/cm) into SVG path data, flipping y so the SVG is upright,
cropped to the page's ArtBox. Colours are left out: the logo is one colour, white.

    python3 design/pdf2svg_paths.py <file.ai|file.pdf> <out.svg>
"""
import re
import sys
import zlib


def content_stream(data):
    page = re.search(rb"/Type\s*/Page\b(?!s)", data)
    contents = re.search(rb"/Contents\s+(\d+)\s+0\s+R", data)
    if not page or not contents:
        raise SystemExit("no page content found")
    num = contents.group(1)
    m = re.search(rb"\b" + num + rb" 0 obj\s*<<(.*?)>>\s*stream\r?\n", data, re.S)
    raw = data[m.end():data.find(b"endstream", m.end())]
    return zlib.decompress(raw) if b"FlateDecode" in m.group(1) else raw


def box(data, name):
    m = re.search(rb"/" + name + rb"\s*\[\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*\]", data)
    return [float(x) for x in m.groups()] if m else None


def mul(a, b):
    """PDF matrix product a x b, matrices as [a b c d e f]."""
    return [a[0] * b[0] + a[1] * b[2], a[0] * b[1] + a[1] * b[3],
            a[2] * b[0] + a[3] * b[2], a[2] * b[1] + a[3] * b[3],
            a[4] * b[0] + a[5] * b[2] + b[4], a[4] * b[1] + a[5] * b[3] + b[5]]


def main(src, dst):
    data = open(src, "rb").read()
    stream = content_stream(data).decode("latin-1")
    x0, y0, x1, y1 = box(data, b"ArtBox") or box(data, b"MediaBox")
    ctm, stack, nums = [1, 0, 0, 1, 0, 0], [], []
    current, d, paths = (0.0, 0.0), [], []

    def pt(x, y):
        px = ctm[0] * x + ctm[2] * y + ctm[4]
        py = ctm[1] * x + ctm[3] * y + ctm[5]
        return "%.3f %.3f" % (px, y1 + y0 - py)   # flip y inside the ArtBox

    for tok in re.findall(r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?|[A-Za-z*'\"]+|/[^\s/\[\]<>()]+", stream):
        if re.fullmatch(r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?", tok):
            nums.append(float(tok))
            continue
        op, a = tok, nums
        if op == "q":
            stack.append(ctm[:])
        elif op == "Q":
            ctm = stack.pop()
        elif op == "cm":
            ctm = mul(a[-6:], ctm)
        elif op == "m":
            d.append("M" + pt(a[-2], a[-1])); current = (a[-2], a[-1])
        elif op == "l":
            d.append("L" + pt(a[-2], a[-1])); current = (a[-2], a[-1])
        elif op == "c":
            d.append("C%s %s %s" % (pt(a[-6], a[-5]), pt(a[-4], a[-3]), pt(a[-2], a[-1]))); current = (a[-2], a[-1])
        elif op == "v":
            d.append("C%s %s %s" % (pt(*current), pt(a[-4], a[-3]), pt(a[-2], a[-1]))); current = (a[-2], a[-1])
        elif op == "y":
            d.append("C%s %s %s" % (pt(a[-4], a[-3]), pt(a[-2], a[-1]), pt(a[-2], a[-1]))); current = (a[-2], a[-1])
        elif op == "h":
            d.append("Z")
        elif op == "re":
            x, y, w, h = a[-4:]
            d.append("M%sL%sL%sL%sZ" % (pt(x, y), pt(x + w, y), pt(x + w, y + h), pt(x, y + h)))
        elif op in ("f", "F", "f*", "B", "B*", "b", "b*"):
            if d:
                paths.append(("evenodd" if op.endswith("*") else "nonzero", "".join(d)))
            d = []
        elif op in ("n", "W", "W*", "S", "s"):
            d = []   # clipping and strokes: not part of the logo's fill
        nums = []
    w, h = x1 - x0, y1 - y0
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%.3f" height="%.3f" viewBox="%.3f %.3f %.3f %.3f">'
           % (w, h, x0, y0, w, h),
           "<!-- Paths copied unchanged from %s by design/pdf2svg_paths.py -->" % src.split("/design/brand/")[-1]]
    for rule, path in paths:
        out.append('<path fill-rule="%s" d="%s" fill="#FFFFFF"/>' % (rule, path))
    out.append("</svg>")
    open(dst, "w", encoding="utf-8").write("\n".join(out) + "\n")
    print("wrote", dst, len(paths), "paths")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
