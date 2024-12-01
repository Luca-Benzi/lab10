package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String config, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder configBuild = new Configuration.Builder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(config)))) {
            for(String line = br.readLine(); line != null; line = br.readLine()) {
                StringTokenizer token = new StringTokenizer(line, ":");
                switch (token.nextToken()) {
                    case "minimum":
                        configBuild.setMin(Integer.parseInt(token.nextToken().trim()));
                        break;
                    case "maximum":
                        configBuild.setMax(Integer.parseInt(token.nextToken().trim()));
                        break;
                    case "attempts":
                        configBuild.setAttempts(Integer.parseInt(token.nextToken().trim()));
                        break;
                    default :
                        throw new IllegalArgumentException();
                }
            }
        } catch (final IOException  | IllegalArgumentException e)  {
            System.err.println("Error: " + e.getMessage());
            displayError("Configuration failed, using default values instead");
        }
        final Configuration configuration = configBuild.build();
        if(configuration.isConsistent()) {
            this.model = new DrawNumberImpl(configuration);
        }
        else{
            displayError("Configuration failed, using default values instead");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }
    private void displayError(final String err) {
        for (final DrawNumberView view: views) {
            view.displayError(err);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", 
                new DrawNumberViewImpl(),
                new DrawNumberViewImpl(),
                new PrintStreamView(System.out),
                new PrintStreamView("output.log"));
    }

}
