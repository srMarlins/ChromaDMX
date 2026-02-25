from PIL import Image, ImageDraw

def create_mascot_sprites():
    # Sprite sheet settings
    frame_size = 16
    cols = 6
    rows = 4
    width = cols * frame_size
    height = rows * frame_size

    sheet = Image.new("RGBA", (width, height), (0, 0, 0, 0))

    # Colors
    BODY = (180, 200, 220, 255) # Silver/Light Blue
    BODY_DARK = (140, 160, 180, 255)
    VISOR = (30, 30, 40, 255) # Dark visor
    EYE_GLOW = (0, 255, 255, 255) # Cyan glow
    LIGHT_ORANGE = (255, 165, 0, 255)
    WHITE = (255, 255, 255, 255)

    def draw_robot(draw, x, y, head_offset=0, body_squash=0, eye_state="normal", arm_state="down", extra=None):
        # Body
        draw.rectangle([x+4, y+8+body_squash, x+11, y+14], fill=BODY, outline=BODY_DARK)
        # Head
        hx, hy = x+4, y+2+head_offset
        draw.rectangle([hx, hy, hx+7, hy+6], fill=BODY, outline=BODY_DARK)
        # Visor
        draw.rectangle([hx+1, hy+2, hx+6, hy+4], fill=VISOR)

        # Eye states
        if eye_state == "normal":
            draw.point([hx+3, hy+3], fill=EYE_GLOW)
            draw.point([hx+4, hy+3], fill=EYE_GLOW)
        elif eye_state == "thinking":
            # Will be animated in frames
            pass
        elif eye_state == "happy":
            draw.line([hx+2, hy+3, hx+3, hy+2], fill=EYE_GLOW)
            draw.line([hx+3, hy+2, hx+4, hy+3], fill=EYE_GLOW)
            draw.line([hx+4, hy+3, hx+5, hy+2], fill=EYE_GLOW)
        elif eye_state == "alert":
            draw.rectangle([hx+3, hy+2, hx+4, hy+4], fill=(255, 0, 0, 255))

        # Arms
        if arm_state == "down":
            draw.line([x+3, y+9+body_squash, x+3, y+12], fill=BODY_DARK)
            draw.line([x+12, y+9+body_squash, x+12, y+12], fill=BODY_DARK)
        elif arm_state == "up":
            draw.line([x+3, y+9+body_squash, x+2, y+6], fill=BODY_DARK)
            draw.line([x+12, y+9+body_squash, x+13, y+6], fill=BODY_DARK)

        if extra == "exclamation":
            draw.rectangle([x+7, y-2, x+8, y+0], fill=(255, 0, 0, 255))
            draw.point([x+7, y+2], fill=(255, 0, 0, 255))
        if extra == "sparkle":
             draw.point([x+2, y+2], fill=WHITE)
             draw.point([x+13, y+3], fill=WHITE)

    draw = ImageDraw.Draw(sheet)

    # Row 0: Idle (4 frames)
    for i in range(4):
        offset = 0 if i % 2 == 0 else 1
        draw_robot(draw, i*frame_size, 0, head_offset=offset, body_squash=offset)

    # Row 1: Thinking (4 frames)
    for i in range(4):
        x = i * frame_size
        y = frame_size
        draw_robot(draw, x, y, eye_state="thinking")
        # Spinning dot
        hx, hy = x+4, y+2
        dots = [(hx+2, hy+3), (hx+3, hy+2), (hx+5, hy+3), (hx+3, hy+4)]
        draw.point(dots[i % 4], fill=EYE_GLOW)

    # Row 2: Happy (4 frames)
    for i in range(4):
        x = i * frame_size
        y = 2 * frame_size
        jump = -2 if i % 2 == 1 else 0
        draw_robot(draw, x, y+jump, eye_state="happy", arm_state="up" if i % 2 == 1 else "down", extra="sparkle")

    # Row 3: Alert (3 frames)
    for i in range(3):
        x = i * frame_size
        y = 3 * frame_size
        draw_robot(draw, x, y, eye_state="alert", extra="exclamation" if i == 1 else None)

    # Row 0 (cont): Confused (3 frames, cols 4,5, row 0)
    for i in range(3):
        x = (i + 3) * frame_size
        y = 3 * frame_size
        # Reuse Row 3's end for Confused
        # Head tilt
        hx, hy = x+4, y+2
        sheet.paste(sheet.crop((x, y, x+15, y+15)), (x, y)) # dummy
        # Actually just draw it
        draw.rectangle([x+4, y+8, x+11, y+14], fill=BODY, outline=BODY_DARK)
        # Head tilted
        draw.rectangle([hx+1, hy, hx+8, hy+6], fill=BODY, outline=BODY_DARK)
        draw.rectangle([hx+2, hy+2, hx+7, hy+4], fill=VISOR)
        draw.point([hx+4, hy+3], fill=EYE_GLOW)

    # Row 0 (cont): Dancing (6 frames, use last row or remaining space)
    # Let's put Dancing on a new sheet or just use the remaining space.
    # We have 6x4 = 24 frames.
    # Idle: 4
    # Thinking: 4
    # Happy: 4
    # Alert: 3
    # Confused: 3
    # Dancing: 6
    # Total = 24. Perfect.

    # Distribution:
    # R0: Idle (4), Dancing (2)
    # R1: Thinking (4), Dancing (2)
    # R2: Happy (4), Dancing (2)
    # R3: Alert (3), Confused (3)

    # Let's redo the loop to be more organized.
    sheet = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)

    # Idle (4)
    for i in range(4):
        offset = 1 if i % 2 == 1 else 0
        draw_robot(draw, i*16, 0, head_offset=offset)

    # Thinking (4)
    for i in range(4):
        draw_robot(draw, i*16, 16, eye_state="thinking")
        hx, hy = i*16+4, 16+2
        dots = [(hx+2, hy+3), (hx+3, hy+2), (hx+5, hy+3), (hx+4, hy+4)]
        draw.point(dots[i % 4], fill=EYE_GLOW)

    # Happy (4)
    for i in range(4):
        jump = -2 if i % 2 == 1 else 0
        draw_robot(draw, i*16, 32+jump, eye_state="happy", arm_state="up" if i % 2 == 1 else "down", extra="sparkle")

    # Alert (3)
    for i in range(3):
        draw_robot(draw, i*16, 48, eye_state="alert", extra="exclamation" if i == 1 else None)

    # Confused (3)
    for i in range(3):
        x, y = (i+3)*16, 48
        # Body
        draw.rectangle([x+4, y+8, x+11, y+14], fill=BODY, outline=BODY_DARK)
        # Head tilted
        hx, hy = x+4, y+2
        draw.rectangle([hx+1, hy, hx+8, hy+6], fill=BODY, outline=BODY_DARK)
        draw.rectangle([hx+2, hy+2, hx+7, hy+4], fill=VISOR)
        draw.point([hx+4, hy+3], fill=EYE_GLOW)

    # Dancing (6) - use columns 4,5 of R0, R1, R2
    for i in range(6):
        r = i // 2
        c = 4 + (i % 2)
        x, y = c*16, r*16
        tilt = -1 if i % 2 == 0 else 1
        # Body
        draw.rectangle([x+4+tilt, y+8, x+11+tilt, y+14], fill=BODY, outline=BODY_DARK)
        # Head
        draw.rectangle([x+4, y+2, x+11, y+8], fill=BODY, outline=BODY_DARK)
        draw.rectangle([x+5, y+4, x+10, y+6], fill=VISOR)
        draw.point([x+7, y+5], fill=EYE_GLOW)
        # Arms up
        draw.line([x+3+tilt, y+9, x+2+tilt, y+5], fill=BODY_DARK)
        draw.line([x+12+tilt, y+9, x+13+tilt, y+5], fill=BODY_DARK)

    sheet.save("shared/src/commonMain/composeResources/drawable/mascot_sprites.png")

create_mascot_sprites()
