package visibility;

import visibility.compat.Compat121;
import visibility.compat.VersionCompat;

/**
 * Plugin entry point for Minecraft 1.21.x.
 */
public class VisibilityPlugin extends VisibilityPluginBase {

    @Override
    protected VersionCompat createCompat() {
        return new Compat121(this);
    }
}
