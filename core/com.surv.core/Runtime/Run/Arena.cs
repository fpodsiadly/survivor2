using System;
using System.Collections.Generic;
using System.Linq;
using Surv.Core.Common;

namespace Surv.Core.Run
{
    public enum Side
    {
        Top,
        Bottom,
        Left,
        Right,
    }

    /// <summary>
    /// World layout of a run (SPEC W3, W7, W8, W19; PLAN P3/P4/P16). One unit of "ekran" = view width = 10 units
    /// at the 9:16 reference; the view is always 10 wide, taller phones see more height.
    /// </summary>
    public sealed class ArenaLayout
    {
        public const float ViewWidth = 10f;
        private readonly float _viewHeight;

        /// <param name="aspect">Screen height / width, e.g. 16/9 = 1.78 or 20/9 = 2.22.</param>
        public ArenaLayout(MapFormat format, float aspect, float spawnMargin = 1f, float relocateRadiusFactor = 1.6f)
        {
            if (aspect < 1) throw new ArgumentOutOfRangeException(nameof(aspect), "Portrait only (SPEC W2).");
            Format = format;
            _viewHeight = ViewWidth * aspect;
            SpawnMargin = spawnMargin;
            RelocateRadius = relocateRadiusFactor * Math.Max(ViewWidth, _viewHeight) / 2f;
        }

        public MapFormat Format { get; }
        public Vec2 ViewHalfSize => new Vec2(ViewWidth / 2f, _viewHeight / 2f);
        public float SpawnMargin { get; }

        /// <summary>Enemies farther than this from the player are relocated (SPEC W19).</summary>
        public float RelocateRadius { get; }

        /// <summary>Closed map: 3 × 3 screens (SPEC W7).</summary>
        public Rect2 ClosedBounds => new Rect2(Vec2.Zero, new Vec2(1.5f * ViewWidth, 1.5f * _viewHeight));

        /// <summary>Corridor: 1.5 screens wide, infinite vertically (SPEC W8).</summary>
        public float CorridorHalfWidth => 0.75f * ViewWidth;

        /// <summary>Where the player may stand.</summary>
        public Vec2 ClampPlayer(Vec2 position)
        {
            switch (Format)
            {
                case MapFormat.Closed:
                    return ClosedBounds.Clamp(position);
                case MapFormat.Corridor:
                    return new Vec2(Math.Max(-CorridorHalfWidth, Math.Min(CorridorHalfWidth, position.X)), position.Y);
                default:
                    return position;
            }
        }

        /// <summary>
        /// Camera center for a player position: open map follows; closed map stays inside the bounds;
        /// corridor keeps X at the corridor center unless the player would leave the view (PLAN P3).
        /// </summary>
        public Vec2 CameraFor(Vec2 player)
        {
            switch (Format)
            {
                case MapFormat.Closed:
                    var b = ClosedBounds;
                    var inner = new Rect2(b.Center, new Vec2(b.HalfSize.X - ViewHalfSize.X, b.HalfSize.Y - ViewHalfSize.Y));
                    return inner.Clamp(player);
                case MapFormat.Corridor:
                    var slack = CorridorHalfWidth - ViewHalfSize.X;
                    var x = Math.Max(-slack, Math.Min(slack, player.X));
                    return new Vec2(x, player.Y);
                default:
                    return player;
            }
        }

        public Rect2 ViewRect(Vec2 camera) => new Rect2(camera, ViewHalfSize);

        /// <summary>Sides a horde enters from (SPEC W11): open ≥3 of 4, corridor both ends, closed ≥3 map edges.</summary>
        public IReadOnlyList<Side> HordeSides(IRandom random)
        {
            if (Format == MapFormat.Corridor) return new[] { Side.Top, Side.Bottom };
            var all = new List<Side> { Side.Top, Side.Bottom, Side.Left, Side.Right };
            if (random.Next(0, 2) == 0) return all;
            all.RemoveAt(random.Next(0, all.Count));
            return all;
        }

        /// <summary>A spawn point just outside the view on the given side (closed map: on the map edge).</summary>
        public Vec2 SpawnPoint(Vec2 camera, Side side, IRandom random)
        {
            var outer = Format == MapFormat.Closed
                ? ClosedBounds
                : new Rect2(camera, new Vec2(ViewHalfSize.X + SpawnMargin, ViewHalfSize.Y + SpawnMargin));
            var t = (float)random.NextDouble();
            Vec2 point;
            switch (side)
            {
                case Side.Top: point = new Vec2(Lerp(outer.MinX, outer.MaxX, t), outer.MaxY); break;
                case Side.Bottom: point = new Vec2(Lerp(outer.MinX, outer.MaxX, t), outer.MinY); break;
                case Side.Left: point = new Vec2(outer.MinX, Lerp(outer.MinY, outer.MaxY, t)); break;
                default: point = new Vec2(outer.MaxX, Lerp(outer.MinY, outer.MaxY, t)); break;
            }
            return Format == MapFormat.Corridor ? ClampPlayer(point) : point;
        }

        /// <summary>
        /// SPEC W19: a far enemy moves just past the view edge on the side the player moves to (random side when
        /// standing). Corridor: to the nearer open end in the move direction. Closed map: never relocated (null).
        /// </summary>
        public Vec2? Relocate(Vec2 enemy, Vec2 player, Vec2 camera, Vec2 moveDirection, IRandom random)
        {
            if (Format == MapFormat.Closed) return null;
            if (Vec2.Distance(enemy, player) <= RelocateRadius) return null;

            var dir = moveDirection.Normalized;
            if (Format == MapFormat.Corridor)
            {
                var up = dir.LengthSquared > 0 ? dir.Y >= 0 : random.Next(0, 2) == 0;
                return SpawnPoint(camera, up ? Side.Top : Side.Bottom, random);
            }
            return SpawnPoint(camera, dir.LengthSquared > 0 ? SideOf(dir) : (Side)random.Next(0, 4), random);
        }

        /// <summary>The view side a direction points at.</summary>
        public static Side SideOf(Vec2 direction) =>
            Math.Abs(direction.X) > Math.Abs(direction.Y)
                ? (direction.X > 0 ? Side.Right : Side.Left)
                : (direction.Y > 0 ? Side.Top : Side.Bottom);

        private static float Lerp(float a, float b, float t) => a + (b - a) * t;
    }

    /// <summary>Caps living enemies (SPEC "Wartości bazowe": 400); the surplus waits in a queue.</summary>
    public sealed class SpawnQueue
    {
        private readonly Queue<string> _waiting = new Queue<string>();

        public SpawnQueue(int maxAlive = 400)
        {
            if (maxAlive <= 0) throw new ArgumentOutOfRangeException(nameof(maxAlive));
            MaxAlive = maxAlive;
        }

        public int MaxAlive { get; }
        public int Alive { get; private set; }
        public int Waiting => _waiting.Count;

        /// <summary>Requests spawns; returns the enemy ids that may spawn now (in request order).</summary>
        public IReadOnlyList<string> Request(IEnumerable<string> enemyIds)
        {
            foreach (var id in enemyIds) _waiting.Enqueue(id);
            return Release();
        }

        /// <summary>Call when an enemy dies or is removed; returns ids that may now spawn.</summary>
        public IReadOnlyList<string> OnRemoved(int count = 1)
        {
            Alive = Math.Max(0, Alive - count);
            return Release();
        }

        /// <summary>Boss entry clears the field and the queue (SPEC W10).</summary>
        public void ClearAll()
        {
            Alive = 0;
            _waiting.Clear();
        }

        private IReadOnlyList<string> Release()
        {
            var released = new List<string>();
            while (Alive < MaxAlive && _waiting.Count > 0)
            {
                released.Add(_waiting.Dequeue());
                Alive++;
            }
            return released;
        }
    }

    /// <summary>Boss arena circle (SPEC W10): centered on the player at boss entry, diameter ≈ 80% of view width.</summary>
    public sealed class BossArena
    {
        public BossArena(Vec2 center, float diameterFraction = 0.8f)
        {
            Center = center;
            Radius = ArenaLayout.ViewWidth * diameterFraction / 2f;
        }

        public Vec2 Center { get; }
        public float Radius { get; }

        /// <summary>Keeps a point (player or boss) inside the circle.</summary>
        public Vec2 Clamp(Vec2 position, float bodyRadius = 0)
        {
            var limit = Math.Max(0, Radius - bodyRadius);
            var offset = position - Center;
            return offset.Length <= limit ? position : Center + offset.Normalized * limit;
        }
    }
}
