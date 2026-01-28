/**
 * Creates a Base64 codec used for decoding (all modes) and encoding in URL-unsafe mode.
 * <p>
 * Default constructor creates an instance with:
 * - No line separation (chunk size = 0)
 * - Standard chunk separator
 * - URL-unsafe encoding (standard Base64 encoding)
 * </p>
 * 
 * <p>
 * When decoding all variants are supported.
 * </p>
 */
public Base64() {
    this(0); // Default to no chunking and standard encoding
}