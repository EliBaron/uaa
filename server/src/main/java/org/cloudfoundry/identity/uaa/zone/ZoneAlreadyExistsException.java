
package org.cloudfoundry.identity.uaa.zone;

import org.cloudfoundry.identity.uaa.error.UaaException;

public class ZoneAlreadyExistsException extends UaaException {

    public ZoneAlreadyExistsException(String msg) {
        super("zone_exists", msg, 409);
    }

    public ZoneAlreadyExistsException(String msg, Throwable cause) {
        super(cause, "zone_exists", msg, 409);
    }
}
