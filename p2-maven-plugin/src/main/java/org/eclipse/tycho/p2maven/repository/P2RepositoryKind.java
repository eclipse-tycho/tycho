package org.eclipse.tycho.p2maven.repository;

public enum P2RepositoryKind {
    /** Kind representing a repository with both, metadata and artifacts. */
    both,
    /** Kind representing a metadata only repository. */
    metadata,
    /** Kind representing a artifact only repository. */
    artifact;

	public boolean isArtifact() {
		return this != metadata;
	}

	public boolean isMetadata() {
		return this != artifact;
	}
}
