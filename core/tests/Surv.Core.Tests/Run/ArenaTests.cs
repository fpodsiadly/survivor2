using System.Linq;
using NUnit.Framework;
using Surv.Core.Common;
using Surv.Core.Run;

namespace Surv.Core.Tests.Run
{
    public class ArenaLayoutTests
    {
        private static readonly IRandom Rng = new SeededRandom(5);

        [TestCase(16f / 9f)]
        [TestCase(20f / 9f)]
        public void View_is_always_ten_wide_taller_phones_see_more_height(float aspect)
        {
            var layout = new ArenaLayout(MapFormat.Open, aspect);
            Assert.That(layout.ViewHalfSize.X, Is.EqualTo(5));
            Assert.That(layout.ViewHalfSize.Y, Is.EqualTo(5 * aspect).Within(1e-5));
        }

        [Test]
        public void Relocates_past_the_view_edge_on_the_move_side()
        {
            var layout = new ArenaLayout(MapFormat.Open, 16f / 9f);
            var player = Vec2.Zero;
            var far = new Vec2(-40, 0);

            var right = layout.Relocate(far, player, player, new Vec2(1, 0), Rng).Value;
            var up = layout.Relocate(far, player, player, new Vec2(0, 1), Rng).Value;
            var down = layout.Relocate(far, player, player, new Vec2(0.2f, -1), Rng).Value;
            var left = layout.Relocate(new Vec2(40, 0), player, player, new Vec2(-1, 0), Rng).Value;

            Assert.That(right.X, Is.EqualTo(layout.ViewHalfSize.X + layout.SpawnMargin).Within(1e-5));
            Assert.That(up.Y, Is.EqualTo(layout.ViewHalfSize.Y + layout.SpawnMargin).Within(1e-5));
            Assert.That(down.Y, Is.EqualTo(-(layout.ViewHalfSize.Y + layout.SpawnMargin)).Within(1e-5));
            Assert.That(left.X, Is.EqualTo(-(layout.ViewHalfSize.X + layout.SpawnMargin)).Within(1e-5));
            Assert.That(layout.ViewRect(player).Contains(right), Is.False, "outside the view");
        }

        [Test]
        public void Standing_player_relocates_to_some_side_outside_the_view()
        {
            var layout = new ArenaLayout(MapFormat.Open, 16f / 9f);
            for (var seed = 0; seed < 50; seed++)
            {
                var p = layout.Relocate(new Vec2(50, 50), Vec2.Zero, Vec2.Zero, Vec2.Zero, new SeededRandom(seed)).Value;
                Assert.That(layout.ViewRect(Vec2.Zero).Contains(p), Is.False);
            }
        }

        [Test]
        public void Near_enemies_and_closed_map_are_never_relocated()
        {
            var open = new ArenaLayout(MapFormat.Open, 16f / 9f);
            var closed = new ArenaLayout(MapFormat.Closed, 16f / 9f);

            Assert.That(open.Relocate(new Vec2(3, 3), Vec2.Zero, Vec2.Zero, new Vec2(1, 0), Rng), Is.Null);
            Assert.That(closed.Relocate(new Vec2(500, 0), Vec2.Zero, Vec2.Zero, new Vec2(1, 0), Rng), Is.Null);
        }

        [Test]
        public void Corridor_relocates_to_the_open_end_in_the_move_direction_inside_the_walls()
        {
            var layout = new ArenaLayout(MapFormat.Corridor, 16f / 9f);
            var p = layout.Relocate(new Vec2(0, -60), Vec2.Zero, Vec2.Zero, new Vec2(0.9f, 0.1f), Rng).Value;

            Assert.That(p.Y, Is.GreaterThan(layout.ViewHalfSize.Y));
            Assert.That(System.Math.Abs(p.X), Is.LessThanOrEqualTo(layout.CorridorHalfWidth));
        }

        [Test]
        public void Horde_sides_per_format()
        {
            var corridor = new ArenaLayout(MapFormat.Corridor, 2f);
            Assert.That(corridor.HordeSides(Rng), Is.EquivalentTo(new[] { Side.Top, Side.Bottom }));

            foreach (var format in new[] { MapFormat.Open, MapFormat.Closed })
            {
                var layout = new ArenaLayout(format, 2f);
                for (var seed = 0; seed < 50; seed++)
                {
                    var sides = layout.HordeSides(new SeededRandom(seed));
                    Assert.That(sides.Count, Is.GreaterThanOrEqualTo(3));
                    Assert.That(sides.Distinct().Count(), Is.EqualTo(sides.Count));
                }
            }
        }

        [Test]
        public void Closed_map_clamps_player_and_camera_to_the_bounds()
        {
            var layout = new ArenaLayout(MapFormat.Closed, 16f / 9f);
            var b = layout.ClosedBounds;

            var player = layout.ClampPlayer(new Vec2(1000, -1000));
            var camera = layout.CameraFor(player);

            Assert.That(player.X, Is.EqualTo(b.MaxX));
            Assert.That(player.Y, Is.EqualTo(b.MinY));
            Assert.That(camera.X + layout.ViewHalfSize.X, Is.EqualTo(b.MaxX).Within(1e-4), "view edge on the wall, not past it");
            Assert.That(camera.Y - layout.ViewHalfSize.Y, Is.EqualTo(b.MinY).Within(1e-4));
        }

        [Test]
        public void Corridor_keeps_player_between_walls_and_visible()
        {
            var layout = new ArenaLayout(MapFormat.Corridor, 16f / 9f);
            var player = layout.ClampPlayer(new Vec2(99, 12));
            var camera = layout.CameraFor(player);

            Assert.That(player.X, Is.EqualTo(layout.CorridorHalfWidth));
            Assert.That(player.Y, Is.EqualTo(12));
            Assert.That(layout.ViewRect(camera).Contains(player), Is.True);
        }
    }

    public class SpawnQueueAndBossArenaTests
    {
        [Test]
        public void Surplus_waits_and_spawns_when_enemies_die()
        {
            var queue = new SpawnQueue(maxAlive: 3);

            var first = queue.Request(new[] { "a", "b", "c", "d", "e" });
            Assert.That(first, Is.EqualTo(new[] { "a", "b", "c" }));
            Assert.That(queue.Waiting, Is.EqualTo(2));

            Assert.That(queue.OnRemoved(), Is.EqualTo(new[] { "d" }));
            queue.ClearAll();
            Assert.That(queue.Alive, Is.Zero);
            Assert.That(queue.Waiting, Is.Zero);
        }

        [Test]
        public void Arena_circle_is_eighty_percent_of_view_width_and_keeps_the_player_in()
        {
            var arena = new BossArena(new Vec2(10, 10));

            Assert.That(arena.Radius, Is.EqualTo(4));
            var clamped = arena.Clamp(new Vec2(30, 10));
            Assert.That(clamped.X, Is.EqualTo(14).Within(1e-5));
            Assert.That(arena.Clamp(new Vec2(11, 11)), Is.EqualTo(new Vec2(11, 11)));
        }
    }
}
