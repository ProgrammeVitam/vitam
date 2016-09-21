Digest
######

Ce package a pour objet de permettre les calculs d'empreintes au sein de Vitam.

Les formats supportés sont :

* MD5
* SHA-1
* SHA-256
* SHA-384
* SHA-512


Usage
*****

.. code-block:: java

    Digest digest = new Digest(DigestType.MD5);
    // One of
    digest.update(File);
    digest.update(byte []);
    digest.update(ByteBuffer);
    digest.update(String);
    digest.update(InputStream);
    digest.update(FileChannel);
    
    // Or using helpers
    Digest digest = Digest.digest(InputStream, DigestType);
    Digest digest = Digest.digest(File, DigestType);
    
    // Get the result
    byte[] bresult = digest.digest();
    String sresult = digest.digestHex(); // in Hexa format
    String sresult = digest.toString(); // in Hexa format
    
    // Compare the result: Note that only same DigestType can be used
    boolean same = digest.equals(digest2);
    boolean same = digest.equals(bresult);
    boolean same = digest.equals(sresult);
    boolean same = digest.equalsWithType(bresult, DigestType); // same as equals(bresult)
    boolean same = digest.equalsWithType(sresult, DigestType); // same as equals(sresult)

