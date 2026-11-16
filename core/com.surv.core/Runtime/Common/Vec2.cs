using System;

namespace Surv.Core.Common
{
    /// <summary>Minimal 2D vector for engine-independent rules (Burst-friendly: blittable, no managed fields).</summary>
    public readonly struct Vec2 : IEquatable<Vec2>
    {
        public Vec2(float x, float y)
        {
            X = x;
            Y = y;
        }

        public float X { get; }
        public float Y { get; }

        public static Vec2 Zero => new Vec2(0, 0);

        public float Length => (float)Math.Sqrt(X * X + Y * Y);
        public float LengthSquared => X * X + Y * Y;

        public Vec2 Normalized
        {
            get
            {
                var length = Length;
                return length < 1e-6f ? Zero : new Vec2(X / length, Y / length);
            }
        }

        public static Vec2 operator +(Vec2 a, Vec2 b) => new Vec2(a.X + b.X, a.Y + b.Y);
        public static Vec2 operator -(Vec2 a, Vec2 b) => new Vec2(a.X - b.X, a.Y - b.Y);
        public static Vec2 operator *(Vec2 a, float k) => new Vec2(a.X * k, a.Y * k);

        public static float Distance(Vec2 a, Vec2 b) => (a - b).Length;

        public bool Equals(Vec2 other) => X.Equals(other.X) && Y.Equals(other.Y);
        public override bool Equals(object obj) => obj is Vec2 other && Equals(other);
        public override int GetHashCode() => (X.GetHashCode() * 397) ^ Y.GetHashCode();
        public override string ToString() => $"({X:0.##}, {Y:0.##})";
    }

    /// <summary>Axis-aligned rectangle given by its center and half size.</summary>
    public readonly struct Rect2
    {
        public Rect2(Vec2 center, Vec2 halfSize)
        {
            Center = center;
            HalfSize = halfSize;
        }

        public Vec2 Center { get; }
        public Vec2 HalfSize { get; }
        public float MinX => Center.X - HalfSize.X;
        public float MaxX => Center.X + HalfSize.X;
        public float MinY => Center.Y - HalfSize.Y;
        public float MaxY => Center.Y + HalfSize.Y;

        public bool Contains(Vec2 p) => p.X >= MinX && p.X <= MaxX && p.Y >= MinY && p.Y <= MaxY;

        public Vec2 Clamp(Vec2 p) => new Vec2(Math.Max(MinX, Math.Min(MaxX, p.X)), Math.Max(MinY, Math.Min(MaxY, p.Y)));
    }
}
