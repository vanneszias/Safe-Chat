use x25519_dalek::{EphemeralSecret, PublicKey as X25519PublicKey};
use base64::{engine::general_purpose, Engine as _};
use rand_core::OsRng;

/// Generates a new ephemeral X25519 keypair and returns the public key as a Base64-encoded string.
///
/// The function creates a cryptographically secure ephemeral secret key, derives its corresponding public key,
/// and encodes the public key bytes using the standard Base64 encoding scheme. The secret key is not exposed.
///
/// # Examples
///
/// ```
/// let public_key_b64 = generate_keypair_base64();
/// assert!(!public_key_b64.is_empty());
/// ```
pub fn generate_keypair_base64() -> String {
    let secret = EphemeralSecret::random_from_rng(OsRng);
    let public = X25519PublicKey::from(&secret);
    general_purpose::STANDARD.encode(public.as_bytes())
} 