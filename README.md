# What is Helix?
Helix is a Java exploitation framework centered around the simplicity and speed of use by bringing most of the boring overhead within a single method invocation so you can focus on exploiting the target machine.

# NOTICE: Still in development
Helix is still under development so some (or most) features might not be available yet.

# Features
> **NOTICE: The list of features isn't yet complete. A lot more exciting features will be added in the future. All feature recommendations / requests are welcome.**

## Simple yet powerful network layer
- Lightweight TCP command & control (CNC) protocol
- UDP layer for live video, live audio, remote desktop, etc.
- Tor layer; all network layer traffic can be optionally routed through Tor
- VPS layer; all network layer traffic can be optionally routed through a VPN
- SOCKS / HTTP proxy support; all network layer traffic can be optionally routed through a SOCKS / HTTP proxy
## Rich toolkit
- Lightweight HTTP client which can be tunneled through Tor, a VPN or a proxy
- File digest utilities (SHA-256 file digest, SHA-256 merkle root) provide a fast and easy way of checking the integrity of your _Genome's_ resources
- Zip utilities (compress or extract a zip file with a single method call)
- **TBD**
## Genomes
- Helix applications are called _Genomes_
- Each _Genome_ lives inside a non-executable (or executable if your application requires it) jar file
- _Genomes_ are loaded on request or automatically; Helix can be configured to watch a specific location on your file system (or your entire file system) and automatically attempt to load any new _Genomes_ found in that location
- This approach makes deploying new _Genomes_ as easy as storing the _Genome_ jar file on the target machine

# CAUTION: I'm not responsible for any misuse of Helix. Helix is intended to be used for educational or pentesting purposes only.
