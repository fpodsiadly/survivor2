using System;

namespace Surv.Core.Meta
{
    /// <summary>Language choice (SPEC W44): a saved choice wins; otherwise the system language if Polish or English, else English.</summary>
    public static class Language
    {
        public const string Polish = "pl";
        public const string English = "en";

        /// <param name="saved">"pl", "en" or null.</param>
        /// <param name="systemLanguage">ISO code or name from the device, e.g. "pl", "pl-PL", "Polish", "de".</param>
        public static string Pick(string saved, string systemLanguage)
        {
            if (IsSupported(saved)) return saved;
            var system = Normalize(systemLanguage);
            return system ?? English;
        }

        private static bool IsSupported(string code) => code == Polish || code == English;

        private static string Normalize(string language)
        {
            if (string.IsNullOrWhiteSpace(language)) return null;
            var value = language.Trim();
            if (value.StartsWith("pl", StringComparison.OrdinalIgnoreCase) || value.Equals("Polish", StringComparison.OrdinalIgnoreCase)) return Polish;
            if (value.StartsWith("en", StringComparison.OrdinalIgnoreCase) || value.Equals("English", StringComparison.OrdinalIgnoreCase)) return English;
            return null;
        }
    }
}
