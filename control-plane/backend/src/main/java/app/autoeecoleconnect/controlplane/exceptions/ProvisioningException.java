package app.autoeecoleconnect.controlplane.exceptions;

/**
 * Levée quand une étape externe du provisioning échoue (commit GitHub,
 * appel API ArgoCD...). Capturée par {@code ProvisioningService} pour marquer
 * le tenant et le log correspondants en statut {@code failed}.
 */
public class ProvisioningException extends RuntimeException {

    public ProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
