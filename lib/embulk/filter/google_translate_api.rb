Embulk::JavaPlugin.register_filter(
  "google_translate_api", "org.embulk.filter.google_translate_api.GoogleTranslateApiFilterPlugin",
  File.expand_path('../../../../classpath', __FILE__))
