using MakerTally.App.Presentation.Services;

namespace MakerTally.App.Controls;

[ContentProperty(nameof(Key))]
[AcceptEmptyServiceProvider]
public sealed class LocalizedTextExtension : IMarkupExtension<BindingBase>
{
    public string Key { get; set; } = "";
    public BindingBase ProvideValue(IServiceProvider serviceProvider)
        => new Binding($"[{Key}]", source: LocalizationService.Instance);
    object IMarkupExtension.ProvideValue(IServiceProvider serviceProvider) => ProvideValue(serviceProvider);
}
