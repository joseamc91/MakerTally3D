namespace MakerTally.App.Controls;

/// <summary>UI-only reflow: three desktop columns, two compact columns, one phone column.</summary>
public sealed class AdaptiveGrid : Grid
{
    public int MaximumColumns { get; set; } = 3;
    private int _columns;
    public AdaptiveGrid() => SizeChanged += (_, _) => Reflow();
    private void Reflow()
    {
        if (Width <= 0) return;
        int columns = Math.Min(MaximumColumns, Width >= 820 ? 3 : Width >= 540 ? 2 : 1);
        if (columns == _columns) return;
        _columns = columns;
        ColumnDefinitions.Clear(); RowDefinitions.Clear();
        for (int i = 0; i < columns; i++) ColumnDefinitions.Add(new(GridLength.Star));
        for (int i = 0; i < (Children.Count + columns - 1) / columns; i++) RowDefinitions.Add(new(GridLength.Auto));
        for (int i = 0; i < Children.Count; i++)
        {
            SetColumn(Children[i], i % columns);
            SetRow(Children[i], i / columns);
        }
    }
}
