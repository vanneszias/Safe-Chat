use base64::{Engine as _, engine::general_purpose};
use rand_core::OsRng;
use x25519_dalek::{EphemeralSecret, PublicKey as X25519PublicKey};

// X.509 ASN.1 header for X25519 public keys
const X25519_X509_HEADER: [u8; 12] = [
    0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
];

pub fn generate_keypair_base64() -> String {
    let secret = EphemeralSecret::random_from_rng(OsRng);
    let public = X25519PublicKey::from(&secret);
    
    // Create X.509-encoded public key
    let raw_key_bytes = public.as_bytes();
    let mut x509_bytes = Vec::with_capacity(X25519_X509_HEADER.len() + raw_key_bytes.len());
    x509_bytes.extend_from_slice(&X25519_X509_HEADER);
    x509_bytes.extend_from_slice(raw_key_bytes);
    
    general_purpose::STANDARD.encode(&x509_bytes)
}

pub fn encode_raw_key_to_x509(raw_key: &[u8; 32]) -> String {
    let mut x509_bytes = Vec::with_capacity(X25519_X509_HEADER.len() + 32);
    x509_bytes.extend_from_slice(&X25519_X509_HEADER);
    x509_bytes.extend_from_slice(raw_key);
    general_purpose::STANDARD.encode(&x509_bytes)
}

pub fn decode_x509_to_raw_key(x509_base64: &str) -> Result<[u8; 32], &'static str> {
    let x509_bytes = general_purpose::STANDARD.decode(x509_base64)
        .map_err(|_| "Invalid base64 encoding")?;
    
    if x509_bytes.len() == 32 {
        // Already raw key, just convert
        let mut raw_key = [0u8; 32];
        raw_key.copy_from_slice(&x509_bytes);
        return Ok(raw_key);
    }
    
    if x509_bytes.len() != X25519_X509_HEADER.len() + 32 {
        return Err("Invalid X.509 key length");
    }
    
    // Verify X.509 header
    if &x509_bytes[..X25519_X509_HEADER.len()] != &X25519_X509_HEADER {
        return Err("Invalid X.509 header for X25519 key");
    }
    
    // Extract raw key
    let mut raw_key = [0u8; 32];
    raw_key.copy_from_slice(&x509_bytes[X25519_X509_HEADER.len()..]);
    Ok(raw_key)
}

pub fn validate_x509_public_key(x509_base64: &str) -> bool {
    decode_x509_to_raw_key(x509_base64).is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_keypair_x509_format() {
        let key_b64 = generate_keypair_base64();
        assert!(validate_x509_public_key(&key_b64));
    }

    #[test]
    fn test_raw_to_x509_conversion() {
        let raw_key = [1u8; 32];
        let x509_b64 = encode_raw_key_to_x509(&raw_key);
        let decoded_raw = decode_x509_to_raw_key(&x509_b64).unwrap();
        assert_eq!(raw_key, decoded_raw);
    }

    #[test]
    fn test_x509_validation() {
        let valid_key = generate_keypair_base64();
        assert!(validate_x509_public_key(&valid_key));
        
        let invalid_key = "invalid_base64";
        assert!(!validate_x509_public_key(invalid_key));
    }
}