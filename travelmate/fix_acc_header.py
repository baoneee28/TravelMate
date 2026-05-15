import re

f = r'c:\TravelMate\travelmate\src\main\resources\templates\admin\accommodations.html'
with open(f, 'r', encoding='utf-8') as fh:
    lines = fh.readlines()

# Find and replace lines 139-146 (0-indexed: 138-145)
new_lines = []
skip_until = -1
for i, line in enumerate(lines):
    if skip_until > 0 and i < skip_until:
        continue
    skip_until = -1
    
    if 'class="page-header"' in line:
        # Replace lines 139-146 with new header
        new_lines.append('                <div class="acc-page-header">\r\n')
        new_lines.append('                    <div class="acc-page-header__icon"><i class="fa-solid fa-hotel"></i></div>\r\n')
        new_lines.append('                    <div>\r\n')
        new_lines.append('                        <h2>Qu\u1ea3n l\u00fd &amp; Duy\u1ec7t N\u01a1i L\u01b0u Tr\u00fa</h2>\r\n')
        new_lines.append('                        <p>Duy\u1ec7t ho\u1eb7c t\u1eeb ch\u1ed1i listing do Partner g\u1eedi l\u00ean. Ch\u1ec9 listing <strong>APPROVED</strong> m\u1edbi hi\u1ec3n th\u1ecb cho kh\u00e1ch h\u00e0ng.</p>\r\n')
        new_lines.append('                    </div>\r\n')
        new_lines.append('                </div>\r\n')
        # Skip until we find the closing </div> of page-header
        # Count from current line, skip 7 more lines (140-146)
        skip_until = i + 8
        continue
    
    new_lines.append(line)

with open(f, 'w', encoding='utf-8') as fh:
    fh.writelines(new_lines)

print(f"Done! Wrote {len(new_lines)} lines (was {len(lines)})")
