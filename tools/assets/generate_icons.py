from PIL import Image, ImageDraw

def create_icons():
    icon_size = 16
    base_path = "shared/src/commonMain/composeResources/drawable/"

    def save_icon(name, draw_func):
        img = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        draw_func(draw)
        img.save(f"{base_path}{name}.png")

    # Genre Icons
    save_icon("ic_genre_techno", lambda d: d.rectangle([4, 4, 11, 11], fill=(0, 255, 0, 255))) # Square/Minimal
    save_icon("ic_genre_house", lambda d: d.polygon([(8, 2), (2, 8), (14, 8), (4, 8), (4, 14), (12, 14), (12, 8)], fill=(0, 100, 255, 255)))
    save_icon("ic_genre_dnb", lambda d: d.ellipse([2, 4, 13, 11], outline=(255, 0, 0, 255))) # Drum?
    save_icon("ic_genre_ambient", lambda d: d.rectangle([2, 6, 13, 9], fill=(150, 150, 255, 100))) # Cloud-like
    save_icon("ic_genre_hiphop", lambda d: d.rectangle([4, 10, 11, 13], fill=(255, 255, 0, 255))) # Boombox base
    save_icon("ic_genre_pop", lambda d: d.polygon([(8, 2), (10, 7), (15, 7), (11, 10), (13, 15), (8, 12), (3, 15), (5, 10), (1, 7), (6, 7)], fill=(255, 100, 200, 255))) # Star
    save_icon("ic_genre_rock", lambda d: d.line([4, 14, 4, 2, 12, 14, 12, 2], fill=(255, 0, 0, 255), width=2)) # 'M' shape / Horns
    save_icon("ic_genre_custom", lambda d: d.text((4, 2), "?", fill=(255, 255, 255, 255)))

    # Fixture Type Icons
    save_icon("ic_fixture_par", lambda d: d.ellipse([4, 2, 11, 13], fill=(100, 100, 100, 255))) # Par can
    save_icon("ic_fixture_moving_head", lambda d: (d.rectangle([4, 10, 11, 14], fill=(80, 80, 80, 255)), d.ellipse([3, 2, 12, 9], fill=(120, 120, 120, 255))))
    save_icon("ic_fixture_pixel_bar", lambda d: d.rectangle([1, 6, 14, 9], fill=(50, 50, 50, 255), outline=(200, 200, 200, 255)))
    save_icon("ic_fixture_strobe", lambda d: d.polygon([(8, 1), (14, 8), (8, 15), (2, 8)], fill=(255, 255, 255, 255))) # Flash
    save_icon("ic_fixture_wash", lambda d: d.ellipse([2, 2, 13, 13], fill=(200, 200, 255, 150)))
    save_icon("ic_fixture_spot", lambda d: d.ellipse([5, 5, 10, 10], fill=(255, 255, 255, 255), outline=(100, 100, 100, 255)))

    # Navigation
    save_icon("ic_nav_gear", lambda d: d.ellipse([3, 3, 12, 12], outline=(150, 150, 150, 255), width=2))
    save_icon("ic_nav_chat", lambda d: d.rectangle([2, 3, 13, 10], fill=(200, 200, 200, 255)))
    save_icon("ic_nav_heart", lambda d: d.polygon([(8, 14), (2, 7), (4, 3), (8, 5), (12, 3), (14, 7)], fill=(255, 50, 50, 255)))

    # Status
    save_icon("ic_status_connected", lambda d: d.ellipse([4, 4, 11, 11], fill=(0, 255, 0, 255)))
    save_icon("ic_status_disconnected", lambda d: d.ellipse([4, 4, 11, 11], fill=(255, 0, 0, 255)))
    save_icon("ic_status_simulated", lambda d: d.ellipse([4, 4, 11, 11], fill=(0, 150, 255, 255)))
    save_icon("ic_status_live", lambda d: (d.ellipse([4, 4, 11, 11], fill=(255, 165, 0, 255)), d.point([8, 8], fill=(255, 255, 255, 255))))

create_icons()
