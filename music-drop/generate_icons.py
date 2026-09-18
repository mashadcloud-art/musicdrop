import os
from PIL import Image, ImageOps

source_path = r"C:\Users\PC\.gemini\antigravity-ide\brain\e472b385-2efd-4c03-bc92-e3757a35ed81\.user_uploaded\media_1788962832514.png"
res_dir = r"d:\FileDrop\android-native\app\src\main\res"

img = Image.open(source_path).convert("RGBA")
w, h = img.size
print(f"Source size: {w}x{h}")

# Density sizes for legacy and standard icons (square w x h)
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive foreground sizes (108dp base)
adaptive_densities = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # 1. Standard / Round App Icon (Zoomed out: logo is ~80% of canvas with transparent/clean margin)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    logo_size = int(size * 0.85)
    resized_logo = img.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    offset = ((size - logo_size) // 2, (size - logo_size) // 2)
    canvas.paste(resized_logo, offset, resized_logo)
    
    canvas.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
    canvas.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")

for folder, size in adaptive_densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # 2. Adaptive Foreground (Safe zone: 66% of 108dp canvas so it never gets clipped)
    fg_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # 68% size for nice zoomed out padding
    fg_logo_size = int(size * 0.68)
    fg_resized = img.resize((fg_logo_size, fg_logo_size), Image.Resampling.LANCZOS)
    fg_offset = ((size - fg_logo_size) // 2, (size - fg_logo_size) // 2)
    fg_canvas.paste(fg_resized, fg_offset, fg_resized)
    fg_canvas.save(os.path.join(folder_path, "ic_launcher_foreground.png"), "PNG")

print("Generated all launcher and adaptive icons successfully!")
