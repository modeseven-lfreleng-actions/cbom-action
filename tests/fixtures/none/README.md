<!--
SPDX-License-Identifier: Apache-2.0
SPDX-FileCopyrightText: 2026 The Linux Foundation
-->

# No-language fixture

This directory deliberately contains no source file in any language the
CBOM scanner supports. It backs the test asserting that the action
reports `outcome: skipped` rather than failing when it finds nothing to
scan.

Do not add Java, Python, Go or C# files here.
