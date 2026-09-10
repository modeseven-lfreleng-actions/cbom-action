# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 The Linux Foundation

"""Fixture exercising pyca/cryptography entry points.

Not production code: it exists so the CBOM scan has a known, stable set
of cryptographic assets to find.
"""

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes


def digest(data: bytes) -> bytes:
    """Hash data with SHA-256."""
    hasher = hashes.Hash(hashes.SHA256())
    hasher.update(data)
    return hasher.finalize()


def aes_cipher(key: bytes, iv: bytes) -> Cipher:
    """Build an AES-CBC cipher."""
    return Cipher(algorithms.AES(key), modes.CBC(iv))


def rsa_key() -> rsa.RSAPrivateKey:
    """Generate a 2048-bit RSA private key."""
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)
