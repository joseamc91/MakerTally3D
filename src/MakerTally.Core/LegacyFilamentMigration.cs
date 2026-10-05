namespace MakerTally.Core;

public static class LegacyFilamentMigration
{
    // Infer only an unambiguous extra token between/after the exact legacy type and brand.
    // Unknown names remain untouched in Name. An explicitly saved Variant (even empty) wins.
    public static FilamentProfile AddVariant(FilamentProfile profile)
    {
        if (!string.IsNullOrWhiteSpace(profile.Variant)) return profile;
        string type = profile.MaterialType.Trim(), brand = profile.Brand.Trim(), name = profile.Name.Trim();
        if (type.Length == 0 || brand.Length == 0 || !name.StartsWith(type + " ", StringComparison.OrdinalIgnoreCase)) return profile;
        string rest = name[(type.Length + 1)..].Trim();
        string? variant = rest.StartsWith(brand + " ", StringComparison.OrdinalIgnoreCase) ? rest[(brand.Length + 1)..].Trim()
            : rest.EndsWith(" " + brand, StringComparison.OrdinalIgnoreCase) ? rest[..^(brand.Length + 1)].Trim() : null;
        return string.IsNullOrEmpty(variant) ? profile : profile with { Variant = variant };
    }
}
