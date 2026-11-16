using System;

namespace Surv.Core.Common
{
    /// <summary>Source of randomness for game rules; seeded in tests so draws are reproducible (PLAN.md D7).</summary>
    public interface IRandom
    {
        /// <summary>Returns an integer in [minInclusive, maxExclusive).</summary>
        int Next(int minInclusive, int maxExclusive);

        /// <summary>Returns a double in [0, 1).</summary>
        double NextDouble();
    }

    public sealed class SeededRandom : IRandom
    {
        private readonly Random _random;

        public SeededRandom(int seed)
        {
            _random = new Random(seed);
        }

        public int Next(int minInclusive, int maxExclusive) => _random.Next(minInclusive, maxExclusive);

        public double NextDouble() => _random.NextDouble();
    }
}
