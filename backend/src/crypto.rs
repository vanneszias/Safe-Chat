use base64::{Engine as _, engine::general_purpose};
use rand_core::OsRng;
use x25519_dalek::{EphemeralSecret, PublicKey as X25519PublicKey};

pub fn generate_keypair_base64() -> String {
    let secret = EphemeralSecret::random_from_rng(OsRng);
    let public = X25519PublicKey::from(&secret);
    general_purpose::STANDARD.encode(public.as_bytes())
}
