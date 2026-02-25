from PIL import Image, ImageDraw

def create_onboarding_assets():
    base_path = "shared/src/commonMain/composeResources/drawable/"

    # Logo (32x32)
    logo_size = 32
    logo = Image.new("RGBA", (logo_size, logo_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(logo)
    # Simple 'C' and 'D' pixelated
    draw.rectangle([4, 4, 27, 27], outline=(0, 255, 255, 255), width=2)
    draw.rectangle([8, 8, 23, 23], fill=(255, 0, 255, 100))
    logo.save(f"{base_path}logo_assembled.png")

    # Scattered logo (simple version: just some pixels)
    logo_scattered = Image.new("RGBA", (logo_size, logo_size), (0, 0, 0, 0))
    draw_s = ImageDraw.Draw(logo_scattered)
    import random
    for _ in range(50):
        x, y = random.randint(0, 31), random.randint(0, 31)
        draw_s.point([x, y], fill=(0, 255, 255, 255))
    logo_scattered.save(f"{base_path}logo_scattered.png")

    # Network scan elements
    # Pixel wire (16x16 with a line)
    wire = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_w = ImageDraw.Draw(wire)
    draw_w.line([0, 8, 15, 8], fill=(100, 100, 100, 255), width=1)
    wire.save(f"{base_path}scan_wire.png")

    # Node icon (16x16)
    node = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_n = ImageDraw.Draw(node)
    draw_n.rectangle([4, 4, 11, 11], fill=(0, 200, 0, 255), outline=(255, 255, 255, 255))
    node.save(f"{base_path}scan_node.png")

    # Fixture scan overlay elements
    # Grid lines (repeatable 16x16)
    grid = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_g = ImageDraw.Draw(grid)
    draw_g.line([0, 0, 15, 0], fill=(255, 255, 255, 50))
    draw_g.line([0, 0, 0, 15], fill=(255, 255, 255, 50))
    grid.save(f"{base_path}scan_grid.png")

    # Detection highlight (16x16 circle)
    highlight = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_h = ImageDraw.Draw(highlight)
    draw_h.ellipse([2, 2, 13, 13], outline=(255, 255, 0, 200), width=2)
    highlight.save(f"{base_path}scan_highlight.png")

create_onboarding_assets()
