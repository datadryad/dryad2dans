package org.datadryad.dans;

/**
 * Exception class for handling errors during transfer to DANS
 */
public class DANSTransferException extends Exception
{
    public DANSTransferException()
    {
        super();
    }

    public DANSTransferException(String message)
    {
        super(message);
    }

    public DANSTransferException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DANSTransferException(Throwable cause)
    {
        super(cause);
    }

    protected DANSTransferException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
