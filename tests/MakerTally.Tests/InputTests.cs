using MakerTally.Core;

namespace MakerTally.Tests;

public sealed class InputTests
{
    [Theory]
    [InlineData("6", "6")] [InlineData("6.5", "6.5")] [InlineData("6,5", "6.5")]
    [InlineData(" 0,1349 ", "0.1349")] [InlineData(".5", "0.5")] [InlineData("0", "0")]
    public void AcceptsEitherDecimalSeparator(string text, string expected)
    {
        Assert.True(NumericInput.TryParse(text, out var actual));
        Assert.Equal(decimal.Parse(expected, System.Globalization.CultureInfo.InvariantCulture), actual);
    }
    [Theory]
    [InlineData("")] [InlineData(",")] [InlineData(".")] [InlineData("-")] [InlineData("abc")]
    [InlineData("1,2.3")] [InlineData("1 000")] [InlineData("1e9")]
    [InlineData("999999999999999999999999999999999999999999999")]
    public void InvalidTypingStatesDoNotThrow(string text) => Assert.False(NumericInput.TryParse(text, out _));
}
