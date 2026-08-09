package app.rocat.i18n

/**
 * Every user-visible string in the app, keyed once here and resolved at runtime by the
 * custom i18n layer (see [Strings]). New UI text should be added here first, then given
 * a translation in both [EnglishStrings] and [IndonesianStrings] so the UI never has to
 * hard-code literals.
 */
enum class StringKey(val key: String) {
    // Navigation
    scripts("nav_scripts"),
    playground("nav_playground"),
    settings("nav_settings"),

    // Common
    back("back"),
    cancel("cancel"),
    delete("delete"),
    save("save"),
    active("active"),
    inactive("inactive"),
    noDescription("no_description"),

    // Scripts screen
    addScript("add_script"),
    noScriptsTitle("no_scripts_title"),
    noScriptsBody("no_scripts_body"),
    scriptNotFound("script_not_found"),
    version("version"),

    // Detail screen
    deleteScriptTitle("delete_script_title"),
    deleteScriptBody("delete_script_body"),
    description("description"),
    author("author"),
    icon("icon"),
    id("id"),
    matches("matches"),
    source("source"),
    editSource("edit_source"),
    script("script"),

    // Import screen
    addScriptTitle("add_script_title"),
    importFromUrl("import_from_url"),
    importFromUrlBody("import_from_url_body"),
    scriptUrl("script_url"),
    fetchImport("fetch_import"),
    pasteSource("paste_source"),
    canvasDemo("canvas_demo"),
    loadExample("load_example"),
    scriptSource("script_source"),
    importSource("import_source"),

    // Playground
    selectScript("select_script"),
    noEnabledScripts("no_enabled_scripts"),
    scriptDrivenUi("script_driven_ui"),
    scriptDrivenUiBody("script_driven_ui_body"),
    buildUi("build_ui"),
    running("running"),
    output("output"),
    console("console"),
    copyJson("copy_json"),
    copyText("copy_text"),
    json("json"),
    jsonCopied("json_copied"),
    textCopied("text_copied"),
    videoPreview("video_preview"),
    playVideo("play_video"),
    noVideoPlayer("no_video_player"),
    mediaOutputVideo("media_output_video"),

    // Canvas screen
    blankCanvas("blank_canvas"),
    blankCanvasBody("blank_canvas_body"),
    rerunOnLaunch("rerun_on_launch"),
    rebuildCanvas("rebuild_canvas"),

    // Settings screen
    settingsTitle("settings_title"),
    language("language"),
    languageEnglish("language_english"),
    languageIndonesian("language_indonesian"),
    storage("storage"),
    storageStatus("storage_status"),
    storageConfigured("storage_configured"),
    storageNotConfigured("storage_not_configured"),
    changeStorage("change_storage"),
    chooseStorageFolder("choose_storage_folder"),
    dataManagement("data_management"),
    clearCache("clear_cache"),
    clearCacheConfirm("clear_cache_confirm"),
    clearCookies("clear_cookies"),
    clearCookiesConfirm("clear_cookies_confirm"),
    clearHistory("clear_history"),
    clearHistoryConfirm("clear_history_confirm"),
    cancelDelete("cancel_delete"),
    done("done"),
    cacheCleared("cache_cleared"),
    cookiesCleared("cookies_cleared"),
    historyCleared("history_cleared"),
    storageChanged("storage_changed"),
    storagePermissionDenied("storage_permission_denied"),
    failure("failure"),

    // Storage setup (first launch)
    setupStorageTitle("setup_storage_title"),
    setupStorageBody("setup_storage_body"),
    setupStorageButton("setup_storage_button"),

    // Scrapes
    scrapes("scrapes"),
    scrapeFolderCreated("scrape_folder_created"),
}
