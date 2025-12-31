package controllers;

/**
 * Small interface to decouple UI controllers from global/static application state.
 * Implemented by {@link WidgetApplication} and passed into controllers at construction time.
 */
public interface AppActions {
    void openConfigurationWindow();

    void applyAndCloseConfigurationWindow();

    void closeConfigurationWindow();

    void hideToTray();

    void requestExit();
}


