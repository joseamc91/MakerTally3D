using System.Diagnostics;
using System.Text.Json;

namespace MakerTally.App;

// Pure geometry and file handling, kept with the Windows feature and linked into tests.
public sealed record WindowBounds(int X, int Y, int Width, int Height);

public static class WindowPlacementPolicy
{
    public static WindowBounds Restore(WindowBounds? saved, IReadOnlyList<WindowBounds> workAreas,
        int defaultWidth = 450, int defaultHeight = 760, int minimumWidth = 400, int minimumHeight = 560)
    {
        var areas = workAreas.Where(a => a.Width > 0 && a.Height > 0).ToArray();
        if (areas.Length == 0) throw new ArgumentException("At least one work area is required.", nameof(workAreas));
        bool validSize = saved is { Width: > 0, Height: > 0 };
        var area = validSize ? areas.MaxBy(a => Overlap(saved!, a))! : areas[0];
        int width = Math.Clamp(validSize ? saved!.Width : defaultWidth, Math.Min(minimumWidth, area.Width), area.Width);
        int height = Math.Clamp(validSize ? saved!.Height : defaultHeight, Math.Min(minimumHeight, area.Height), area.Height);
        // Require enough of the window and its title bar to be reachable; otherwise recenter.
        bool visible = validSize && Overlap(saved!, area) >= (long)Math.Min(width, 160) * Math.Min(height, 64)
            && saved!.Y >= area.Y && (long)saved.Y + 32 <= (long)area.Y + area.Height;
        int x = visible ? (int)Math.Clamp((long)saved!.X, area.X, (long)area.X + area.Width - width)
            : area.X + (area.Width - width) / 2;
        int y = visible ? (int)Math.Clamp((long)saved!.Y, area.Y, (long)area.Y + area.Height - height)
            : area.Y + (area.Height - height) / 2;
        return new(x, y, width, height);
    }
    private static long Overlap(WindowBounds a, WindowBounds b)
        => Math.Max(0L, Math.Min((long)a.X + a.Width, (long)b.X + b.Width) - Math.Max(a.X, b.X))
         * Math.Max(0L, Math.Min((long)a.Y + a.Height, (long)b.Y + b.Height) - Math.Max(a.Y, b.Y));
}

public sealed class WindowPlacementStore(string path)
{
    public WindowBounds? Load()
    {
        try { return File.Exists(path) ? JsonSerializer.Deserialize<WindowBounds>(File.ReadAllText(path)) : null; }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException or JsonException)
        { Debug.WriteLine($"Window placement could not be read: {ex.Message}"); return null; }
    }
    public void Save(WindowBounds bounds)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(path))!);
            File.WriteAllText(path + ".tmp", JsonSerializer.Serialize(bounds));
            File.Move(path + ".tmp", path, overwrite: true);
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        { Debug.WriteLine($"Window placement could not be saved: {ex.Message}"); }
    }
}
