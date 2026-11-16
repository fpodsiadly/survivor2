using System;
using System.Collections.Generic;
using System.IO;
using Newtonsoft.Json.Linq;
using NUnit.Framework;
using Surv.Core.Equipment;
using Surv.Core.Save;
using Surv.Core.Tests.Equipment;

namespace Surv.Core.Tests.Save
{
    /// <summary>In-memory file system that can fail on a chosen operation to simulate a crash mid-save.</summary>
    internal sealed class FakeFileSystem : IFileSystem
    {
        public readonly Dictionary<string, string> Files = new Dictionary<string, string>();
        public string FailOn;

        public bool Exists(string path) => Files.ContainsKey(path);
        public string ReadAllText(string path) => Files[path];

        public void WriteAllText(string path, string contents)
        {
            Crash("write:" + path);
            Files[path] = contents;
        }

        public void Move(string from, string to)
        {
            Crash("move:" + from);
            Files[to] = Files[from];
            Files.Remove(from);
        }

        public void Delete(string path) => Files.Remove(path);

        private void Crash(string op)
        {
            if (FailOn != null && op.StartsWith(FailOn, StringComparison.Ordinal)) throw new IOException("simulated crash at " + op);
        }
    }

    public class SaveStoreTests
    {
        private const string Path = "save.json";

        private static SaveData Data(long gold) => new SaveData { Gold = gold, AccountLevel = 3, UnlockedChapter = 2 };

        [Test]
        public void Round_trip_keeps_the_data()
        {
            var fs = new FakeFileSystem();
            var store = new SaveStore(Path, fs);
            var data = Data(123);
            data.Chapters[1] = new ChapterRecord { Won = true, BestWinSeconds = 912.5, BestSurvivalSeconds = 912.5 };
            data.Settings.Language = "pl";

            store.Save(data);
            var loaded = store.Load();

            Assert.That(loaded.Source, Is.EqualTo(SaveSource.Main));
            Assert.That(loaded.Data.Gold, Is.EqualTo(123));
            Assert.That(loaded.Data.Chapters[1].BestWinSeconds, Is.EqualTo(912.5));
            Assert.That(loaded.Data.Settings.Language, Is.EqualTo("pl"));
        }

        [Test]
        public void Missing_file_gives_a_new_save()
        {
            var loaded = new SaveStore(Path, new FakeFileSystem()).Load();
            Assert.That(loaded.Source, Is.EqualTo(SaveSource.New));
            Assert.That(loaded.Data.AccountLevel, Is.EqualTo(1));
        }

        [Test]
        public void Crash_while_writing_the_temp_file_keeps_the_previous_save()
        {
            var fs = new FakeFileSystem();
            var store = new SaveStore(Path, fs);
            store.Save(Data(100));

            fs.FailOn = "write:";
            Assert.Throws<IOException>(() => store.Save(Data(200)));

            Assert.That(store.Load().Data.Gold, Is.EqualTo(100));
        }

        [Test]
        public void Crash_between_the_two_moves_falls_back_to_the_backup()
        {
            var fs = new FakeFileSystem();
            var store = new SaveStore(Path, fs);
            store.Save(Data(100));

            fs.FailOn = "move:" + store.TempPath;
            Assert.Throws<IOException>(() => store.Save(Data(200)));

            var loaded = store.Load();
            Assert.That(fs.Exists(Path), Is.False);
            Assert.That(loaded.Source, Is.EqualTo(SaveSource.Backup));
            Assert.That(loaded.Data.Gold, Is.EqualTo(100));
        }

        [Test]
        public void Corrupt_main_file_loads_the_backup()
        {
            var fs = new FakeFileSystem();
            var store = new SaveStore(Path, fs);
            store.Save(Data(100));
            store.Save(Data(200));
            fs.Files[Path] = "{ not json";

            var loaded = store.Load();

            Assert.That(loaded.Source, Is.EqualTo(SaveSource.Backup));
            Assert.That(loaded.Data.Gold, Is.EqualTo(100));
        }

        [Test]
        public void Version_zero_file_is_migrated()
        {
            var fs = new FakeFileSystem();
            // Hypothetical old format: gold was called "Coins" and there was no Version field.
            fs.Files[Path] = "{ \"Coins\": 77 }";
            var migrations = new Dictionary<int, Func<JObject, JObject>>
            {
                [0] = json =>
                {
                    json["Gold"] = json["Coins"];
                    json.Remove("Coins");
                    return json;
                },
            };

            var loaded = new SaveStore(Path, fs, migrations).Load();

            Assert.That(loaded.Source, Is.EqualTo(SaveSource.Main));
            Assert.That(loaded.Data.Gold, Is.EqualTo(77));
            Assert.That(loaded.Data.Version, Is.EqualTo(SaveData.CurrentVersion));
        }

        [Test]
        public void Disk_file_system_round_trip()
        {
            var dir = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "surv-save-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(dir);
            try
            {
                var store = new SaveStore(System.IO.Path.Combine(dir, "save.json"), new DiskFileSystem());
                store.Save(Data(1));
                store.Save(Data(2));

                Assert.That(store.Load().Data.Gold, Is.EqualTo(2));
                Assert.That(File.Exists(store.BackupPath), Is.True);
                Assert.That(File.Exists(store.TempPath), Is.False);
            }
            finally
            {
                Directory.Delete(dir, recursive: true);
            }
        }

        [Test]
        public void Inventory_survives_save_and_load()
        {
            var (inventory, _, _) = EquipmentTestData.Setup();
            var weapon = inventory.Add(EquipmentTestData.Kunai, Tier.Epic2, level: 12);
            inventory.Add(EquipmentTestData.BeltA, Tier.Good);
            inventory.Equip(weapon.Uid);
            inventory.AddGold(500);
            inventory.AddPlans(Slot.Belt, 7);
            var fs = new FakeFileSystem();
            var store = new SaveStore(Path, fs);
            var save = new SaveData();

            InventoryMapper.Write(inventory, save);
            store.Save(save);
            var restored = InventoryMapper.Read(store.Load().Data, EquipmentTestData.Catalog());

            Assert.That(restored.EquippedIn(Slot.Weapon).Tier, Is.EqualTo(Tier.Epic2));
            Assert.That(restored.EquippedIn(Slot.Weapon).Level, Is.EqualTo(12));
            Assert.That(restored.Gold, Is.EqualTo(500));
            Assert.That(restored.PlansFor(Slot.Belt), Is.EqualTo(7));
            Assert.That(restored.Add(EquipmentTestData.BeltB, Tier.Normal).Uid, Is.GreaterThan(weapon.Uid), "new uids do not collide");
        }
    }
}
