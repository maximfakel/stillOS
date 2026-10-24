#!/usr/bin/env python3
"""Turn a downloaded brand logo SVG into a plain white one for the launcher tiles.

Every visible filled shape is kept as it is (same coordinates, same order) and painted white; nothing is
redrawn. What changes is only the bookkeeping a VectorDrawable cannot read: CSS classes and style attributes
become plain fill attributes, gradients become white, rect / polygon / circle / ellipse become paths, and
clip paths or masks that only frame the whole logo are dropped. Anything else it does not understand stops
the conversion instead of guessing, so a logo is never silently altered.

    python3 design/brand_white.py SOURCE.svg OUT.svg
"""
import re
import sys
import xml.etree.ElementTree as ET

SVG = "{http://www.w3.org/2000/svg}"
SKIP = {"defs", "title", "desc", "metadata", "style", "clipPath", "mask", "linearGradient", "radialGradient"}


def css_rules(root):
    """Simple class rules from <style>: .a{fill:#fff} and .a,.b{...}."""
    rules = {}
    for st in root.iter(SVG + "style"):
        for sel, body in re.findall(r"([^{}]+)\{([^}]*)\}", st.text or ""):
            decl = dict((k.strip(), v.strip()) for k, v in re.findall(r"([\w-]+)\s*:\s*([^;]+)", body))
            for s in sel.split(","):
                s = s.strip()
                if s.startswith("."):
                    rules.setdefault(s[1:], {}).update(decl)
    return rules


def props(el, rules):
    p = {}
    for k in ("fill", "stroke", "fill-rule", "opacity", "fill-opacity", "display", "visibility"):
        if el.get(k) is not None:
            p[k] = el.get(k).strip()
    for c in (el.get("class") or "").split():
        p.update(rules.get(c, {}))
    p.update(dict((k.strip(), v.strip()) for k, v in re.findall(r"([\w-]+)\s*:\s*([^;]+)", el.get("style") or "")))
    return p


def num(el, k, d=0.0):
    v = el.get(k)
    return float(re.sub(r"px$", "", v)) if v else d


def to_path(el):
    tag = el.tag.replace(SVG, "")
    if tag == "path":
        return el.get("d")
    if tag == "rect":
        x, y, w, h = num(el, "x"), num(el, "y"), num(el, "width"), num(el, "height")
        if el.get("rx") or el.get("ry"):
            rx = num(el, "rx", num(el, "ry")); ry = num(el, "ry", rx)
            return (f"M{x+rx},{y}H{x+w-rx}A{rx},{ry} 0 0 1 {x+w},{y+ry}V{y+h-ry}A{rx},{ry} 0 0 1 {x+w-rx},{y+h}"
                    f"H{x+rx}A{rx},{ry} 0 0 1 {x},{y+h-ry}V{y+ry}A{rx},{ry} 0 0 1 {x+rx},{y}Z")
        return f"M{x},{y}H{x+w}V{y+h}H{x}Z"
    if tag in ("polygon", "polyline"):
        pts = re.findall(r"-?[\d.]+(?:e-?\d+)?", el.get("points"))
        pairs = [f"{pts[i]},{pts[i+1]}" for i in range(0, len(pts), 2)]
        return "M" + " L".join(pairs) + "Z"
    if tag in ("circle", "ellipse"):
        cx, cy = num(el, "cx"), num(el, "cy")
        rx = num(el, "r") if tag == "circle" else num(el, "rx")
        ry = num(el, "r") if tag == "circle" else num(el, "ry")
        return f"M{cx+rx},{cy}A{rx},{ry} 0 1 0 {cx-rx},{cy}A{rx},{ry} 0 1 0 {cx+rx},{cy}Z"
    raise ValueError(f"unsupported element <{tag}>")


def walk(el, rules, inherited, out, depth):
    tag = el.tag.replace(SVG, "")
    if tag in SKIP:
        return
    p = dict(inherited)
    own = props(el, rules)
    if own.get("display") == "none" or own.get("visibility") == "hidden":
        return
    for k in ("opacity", "fill-opacity"):
        if k in own and float(own[k]) == 0:
            return
    p.update({k: v for k, v in own.items() if k in ("fill", "fill-rule", "stroke")})
    pad = "  " * depth
    if tag in ("g", "svg", "a"):
        tr = el.get("transform")
        if tag == "svg" and depth > 0:
            raise ValueError("nested <svg>")
        if tr:
            out.append(f'{pad}<g transform="{tr}">')
        for c in el:
            walk(c, rules, p, out, depth + (1 if tr else 0))
        if tr:
            out.append(f"{pad}</g>")
        return
    if p.get("stroke", "none") not in ("none", "transparent"):
        raise ValueError(f"stroked <{tag}> ({p['stroke']}): outline it first")
    if p.get("fill", "black") in ("none", "transparent"):
        return
    rule = ' fill-rule="evenodd"' if p.get("fill-rule") == "evenodd" else ""
    d = to_path(el)
    tr = f' transform="{el.get("transform")}"' if el.get("transform") else ""
    out.append(f'{pad}<path{tr}{rule} d="{d}"/>')


def convert(src, comment):
    root = ET.parse(src).getroot()
    vb = root.get("viewBox")
    if not vb:
        vb = f"0 0 {num(root, 'width')} {num(root, 'height')}"
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" fill="#ffffff">', f"<!-- {comment} -->"]
    walk(root, css_rules(root), {}, out, 0)
    out.append("</svg>")
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    note = sys.argv[3] if len(sys.argv) > 3 else "White version made by design/brand_white.py; shapes unchanged."
    with open(dst, "w", encoding="utf-8") as f:
        f.write(convert(src, note))
