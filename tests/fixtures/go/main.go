// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 The Linux Foundation

// Package main exercises Go standard-library crypto entry points the
// sonar-cryptography plugin detects. Not production code: it exists so
// the CBOM scan has a known, stable set of cryptographic assets to find.
package main

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"fmt"
)

func digest(data []byte) []byte {
	sum := sha256.Sum256(data)
	return sum[:]
}

func aesCipher(key, iv []byte) (cipher.BlockMode, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	return cipher.NewCBCEncrypter(block, iv), nil
}

func rsaKey() (*rsa.PrivateKey, error) {
	return rsa.GenerateKey(rand.Reader, 2048)
}

func main() {
	fmt.Printf("digest length: %d\n", len(digest([]byte("fixture"))))
}
