using System;
using System.Collections.Generic;
using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Surv.Core.Save
{
    /// <summary>File operations the save store needs; swapped for a fake in tests to simulate crashes.</summary>
    public interface IFileSystem
    {
        bool Exists(string path);
        string ReadAllText(string path);
        void WriteAllText(string path, string contents);
        void Move(string from, string to);
        void Delete(string path);
    }

    public sealed class DiskFileSystem : IFileSystem
    {
        public bool Exists(string path) => File.Exists(path);
        public string ReadAllText(string path) => File.ReadAllText(path);

        public void WriteAllText(string path, string contents)
        {
            using (var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.None))
            using (var writer = new StreamWriter(stream))
            {
                writer.Write(contents);
                writer.Flush();
                stream.Flush(true);
            }
        }

        public void Move(string from, string to) => File.Move(from, to);
        public void Delete(string path) => File.Delete(path);
    }

    public enum SaveSource
    {
        Main,
        Backup,
        New,
    }

    public sealed class LoadResult
    {
        public LoadResult(SaveData data, SaveSource source)
        {
            Data = data;
            Source = source;
        }

        public SaveData Data { get; }
        public SaveSource Source { get; }
    }

    /// <summary>
    /// Atomic JSON save (SPEC W42, PLAN D9/P19): write a temp file, keep the previous file as ".bak", then move the
    /// temp file into place. An interrupted save leaves either the previous main file or the backup readable.
    /// Older formats are upgraded by migrations keyed by their version.
    /// </summary>
    public sealed class SaveStore
    {
        private static readonly JsonSerializerSettings Settings = new JsonSerializerSettings
        {
            Formatting = Formatting.Indented,
            TypeNameHandling = TypeNameHandling.None,
            MissingMemberHandling = MissingMemberHandling.Ignore,
        };

        private readonly string _path;
        private readonly IFileSystem _fs;
        private readonly IReadOnlyDictionary<int, Func<JObject, JObject>> _migrations;

        /// <param name="migrations">Migration from version N to N+1, keyed by N.</param>
        public SaveStore(string path, IFileSystem fileSystem, IReadOnlyDictionary<int, Func<JObject, JObject>> migrations = null)
        {
            if (string.IsNullOrEmpty(path)) throw new ArgumentException("Save path is required.", nameof(path));
            _path = path;
            _fs = fileSystem ?? throw new ArgumentNullException(nameof(fileSystem));
            _migrations = migrations ?? new Dictionary<int, Func<JObject, JObject>>();
        }

        public string MainPath => _path;
        public string BackupPath => _path + ".bak";
        public string TempPath => _path + ".tmp";

        public void Save(SaveData data)
        {
            if (data == null) throw new ArgumentNullException(nameof(data));
            data.Version = SaveData.CurrentVersion;
            var json = JsonConvert.SerializeObject(data, Settings);

            _fs.WriteAllText(TempPath, json);
            if (_fs.Exists(_path))
            {
                if (_fs.Exists(BackupPath)) _fs.Delete(BackupPath);
                _fs.Move(_path, BackupPath);
            }
            _fs.Move(TempPath, _path);
        }

        public LoadResult Load()
        {
            var main = TryRead(_path);
            if (main != null) return new LoadResult(main, SaveSource.Main);
            var backup = TryRead(BackupPath);
            if (backup != null) return new LoadResult(backup, SaveSource.Backup);
            return new LoadResult(new SaveData(), SaveSource.New);
        }

        private SaveData TryRead(string path)
        {
            if (!_fs.Exists(path)) return null;
            try
            {
                var json = JObject.Parse(_fs.ReadAllText(path));
                var migrated = Migrate(json);
                return migrated?.ToObject<SaveData>(JsonSerializer.Create(Settings));
            }
            catch (JsonException)
            {
                return null;
            }
        }

        private JObject Migrate(JObject json)
        {
            var version = json.Value<int?>("Version") ?? 0;
            if (version > SaveData.CurrentVersion) return null;
            while (version < SaveData.CurrentVersion)
            {
                if (!_migrations.TryGetValue(version, out var migrate)) return null;
                json = migrate(json);
                version++;
                json["Version"] = version;
            }
            return json;
        }
    }
}
