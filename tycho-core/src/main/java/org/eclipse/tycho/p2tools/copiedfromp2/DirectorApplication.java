/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith - https://bugs.eclipse.org/bugs/show_bug.cgi?id=226401
 *     EclipseSource - ongoing development
 *     Sonatype, Inc. - ongoing development
 *     Pascal Rapicault - Support for bundled macosx 431116
 *     Red Hat, Inc. - support repositories passed via fragments (see bug 378329).Bug 460967
 *     SAP AG - list formatting (bug 423538)
 *     Todor Boev - Software AG
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import static org.eclipse.core.runtime.IStatus.*;
import static org.eclipse.equinox.internal.p2.director.app.Activator.ID;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.StringHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.IArtifactUIServices;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.IInstallableUnitUIServices;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class DirectorApplication implements IApplication, ProvisioningListener {
	public static class AvoidTrustPromptService extends UIServices
			implements IArtifactUIServices, IInstallableUnitUIServices {

		// These are instructions that are common, expected, and both uninteresting and
		// non-threatening.
		private static final Pattern IGNORED_TOUCHPOINT_DATA = Pattern.compile(
				"manifest=Instruction\\[Bundle-SymbolicName: [^ ]+(; singleton:=true)? Bundle-Version: [^,]+( Fragment-Host: [^;]+;bundle-version=\"[^\"]+\")?,null]|" //$NON-NLS-1$
						+ "zipped=Instruction\\[true,null]"); //$NON-NLS-1$

		private final PrintStream out;
		private final ByteArrayOutputStream details;
		private final boolean trustSignedContentOnly;
		private final Set<URI> trustedAuthorityURIs;
		private final Set<String> trustedPGPKeyFingerprints;
		private final Set<String> trustedCertificateFingerprints;

		public AvoidTrustPromptService() {
			this(false, false, null, null, null);
		}

		public AvoidTrustPromptService(boolean verbose, boolean trustSignedContentOnly, Set<URI> trustedAuthorityURIs,
				Set<String> trustedPGPKeys, Set<String> trustedCertificates) {
			if (verbose) {
				this.out = System.out;
				this.details = null;
			} else {
				details = new ByteArrayOutputStream();
				this.out = new PrintStream(details, false, StandardCharsets.UTF_8);
			}
			this.trustSignedContentOnly = trustSignedContentOnly;
			this.trustedAuthorityURIs = trustedAuthorityURIs;
			this.trustedPGPKeyFingerprints = trustedPGPKeys;
			this.trustedCertificateFingerprints = trustedCertificates;
		}

		public void dump() {
			if (details != null) {
				out.close();
				System.out.println(new String(details.toByteArray(), StandardCharsets.UTF_8));
			}
		}

		@Override
		public AuthenticationInfo getUsernamePassword(String location) {
			return null;
		}

		@Override
		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			return null;
		}

		@Deprecated
		@Override
		public TrustInfo getTrustInfo(Certificate[][] untrustedChains, String[] unsignedDetail) {
			throw new UnsupportedOperationException(
					"Use AvoidTrustPromptService.getTrustAuthorityInfo(Map<URI, Set<IInstallableUnit>>, Map<URI, List<Certificate>>)"); //$NON-NLS-1$
		}

		@Deprecated
		@Override
		public TrustInfo getTrustInfo(Certificate[][] untrustedChains, Collection<PGPPublicKey> untrustedPGPKeys,
				String[] unsignedDetail) {

			throw new UnsupportedOperationException(
					"Use AvoidTrustPromptService.getTrustAuthorityInfo(Map<URI, Set<IInstallableUnit>>, Map<URI, List<Certificate>>)"); //$NON-NLS-1$
		}

		@Override
		public TrustAuthorityInfo getTrustAuthorityInfo(Map<URI, Set<IInstallableUnit>> siteIUs,
				Map<URI, List<Certificate>> siteCertificates) {

			Set<URI> trustedAuthorities = new LinkedHashSet<>();
			for (Map.Entry<URI, Set<IInstallableUnit>> entry : siteIUs.entrySet()) {
				URI authority = entry.getKey();
				out.println(NLS.bind(Messages.DirectorApplication_FetchingIUsHeading, authority));
				for (IInstallableUnit iu : entry.getValue()) {
					out.println("  " + iu); //$NON-NLS-1$
					for (ITouchpointData touchpointData : iu.getTouchpointData()) {
						for (Map.Entry<String, ITouchpointInstruction> data : touchpointData.getInstructions()
								.entrySet()) {
							String text = data.toString().replaceAll("\\s+", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$
							if (!IGNORED_TOUCHPOINT_DATA.matcher(text).matches()) {
								out.println("    " + text); //$NON-NLS-1$
							}
						}
					}
				}

				if (trustedAuthorityURIs != null) {
					List<URI> authorityChain = AuthorityChecker.getAuthorityChain(authority);
					for (URI uri : authorityChain) {
						if (trustedAuthorityURIs.contains(uri)) {
							trustedAuthorities.add(authority);
							break;
						}
					}
				}
			}

			return new TrustAuthorityInfo(trustedAuthorityURIs != null ? trustedAuthorities : siteIUs.keySet(), false,
					false);
		}

		@Override
		public TrustInfo getTrustInfo(Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificateChains,
				Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys, Set<IArtifactKey> unsignedArtifacts,
				Map<IArtifactKey, File> artifactFiles) {

			Set<Certificate> trustedCertificates = new LinkedHashSet<>();
			if (untrustedCertificateChains != null) {
				for (Map.Entry<List<Certificate>, Set<IArtifactKey>> entry : untrustedCertificateChains.entrySet()) {
					out.println(Messages.DirectorApplication_CertficateTrustChainHeading);
					List<Certificate> chain = entry.getKey();
					boolean trusted = false;
					for (Certificate certificate : chain) {
						String fingerprint = getFingerprint(certificate);
						if (trustedCertificateFingerprints == null
								|| trustedCertificateFingerprints.contains(fingerprint)) {
							trusted = true;
						}

						// Indent all the lines two more spaces then the lines for the artifacts.
						String text = certificate.toString().replaceAll("(\r?\n)", "$1    "); //$NON-NLS-1$ //$NON-NLS-2$
						out.println("    " + fingerprint + " -> " + text); //$NON-NLS-1$ //$NON-NLS-2$
					}

					for (IArtifactKey artifactKey : entry.getValue()) {
						File artifactFile = artifactFiles.get(artifactKey);
						out.println("  " + artifactKey + " -> " + artifactFile); //$NON-NLS-1$ //$NON-NLS-2$
					}

					if (trusted) {
						trustedCertificates.add(chain.get(0));
					}
				}
			}

			Set<PGPPublicKey> pgpKeys = new LinkedHashSet<>();
			if (untrustedPGPKeys != null) {
				for (Map.Entry<PGPPublicKey, Set<IArtifactKey>> entry : untrustedPGPKeys.entrySet()) {
					PGPPublicKey key = entry.getKey();
					String fingerprint = PGPPublicKeyService.toHexFingerprint(key);
					if (trustedPGPKeyFingerprints == null || trustedPGPKeyFingerprints.contains(fingerprint)) {
						pgpKeys.add(key);
					}

					out.println(NLS.bind(Messages.DirectorApplication_PGPKeysHeading, fingerprint));
					for (IArtifactKey artifactKey : entry.getValue()) {
						File artifactFile = artifactFiles.get(artifactKey);
						out.println("  " + artifactKey + " -> " + artifactFile); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}

			if (unsignedArtifacts != null && !unsignedArtifacts.isEmpty()) {
				out.println(Messages.DirectorApplication_UnsignedHeading);
				for (IArtifactKey artifactKey : unsignedArtifacts) {
					File artifactFile = artifactFiles.get(artifactKey);
					out.println("  " + artifactKey + " -> " + artifactFile); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			return new TrustInfo(trustedCertificates, pgpKeys, false, !trustSignedContentOnly);
		}

		private String getFingerprint(Certificate certificate) {
			try {
				MessageDigest digester = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
				return HexFormat.of().formatHex(digester.digest(certificate.getEncoded()));
			} catch (CertificateEncodingException | NoSuchAlgorithmException e) {
				return "deadbeef"; //$NON-NLS-1$
			}
		}
	}

	class LocationQueryable implements IQueryable<IInstallableUnit> {
		private URI location;

		public LocationQueryable(URI location) {
			this.location = location;
			Assert.isNotNull(location);
		}

		@Override
		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			return getInstallableUnits(location, query, monitor);
		}
	}

	private static class CommandLineOption {
		final String[] identifiers;
		private final String optionSyntaxString;
		private final String helpString;

		CommandLineOption(String[] identifiers, String optionSyntaxString, String helpString) {
			this.identifiers = identifiers;
			this.optionSyntaxString = optionSyntaxString;
			this.helpString = helpString;
		}

		boolean isOption(String opt) {
			int idx = identifiers.length;
			while (--idx >= 0)
				if (identifiers[idx].equalsIgnoreCase(opt))
					return true;
			return false;
		}

		void appendHelp(PrintStream out) {
			out.print(identifiers[0]);
			for (int idx = 1; idx < identifiers.length; ++idx) {
				out.print(" | "); //$NON-NLS-1$
				out.print(identifiers[idx]);
			}
			if (optionSyntaxString != null) {
				out.print(' ');
				out.print(optionSyntaxString);
			}
			out.println();
			out.print("  "); //$NON-NLS-1$
			out.println(helpString);
		}

		@SuppressWarnings("nls")
		void appendHelpDocumentation(PrintStream out) {
			out.print("<dt>");
			out.print(identifiers[0]);
			for (int idx = 1; idx < identifiers.length; ++idx) {
				out.print(" | "); //$NON-NLS-1$
				out.print(identifiers[idx]);
			}
			if (optionSyntaxString != null) {
				out.print(' ');
				out.print(escape(optionSyntaxString));
			}
			out.println("</dt>");
			out.println("<dd>");
			out.println(escape(helpString));
			out.println("</dd>");
		}

		@SuppressWarnings("nls")
		private String escape(String string) {
			return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}

	private static final CommandLineOption OPTION_HELP = new CommandLineOption(new String[] { //
			"-help", "-h", "-?" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			null, Messages.Help_Prints_this_command_line_help);
	private static final CommandLineOption OPTION_LIST = new CommandLineOption(new String[] { //
			"-list", "-l" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lb_lt_comma_separated_list_gt_rb, Messages.Help_List_all_IUs_found_in_repos);
	private static final CommandLineOption OPTION_LIST_FORMAT = new CommandLineOption(new String[] { //
			"-listFormat", "-lf" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_list_format_gt, Messages.Help_formats_the_IU_list);
	private static final CommandLineOption OPTION_LIST_INSTALLED = new CommandLineOption(new String[] { //
			"-listInstalledRoots", "-lir" }, //$NON-NLS-1$ //$NON-NLS-2$
			null, Messages.Help_List_installed_roots);
	private static final CommandLineOption OPTION_INSTALL_IU = new CommandLineOption(new String[] { //
			"-installIU", "-installIUs", "-i" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_Installs_the_listed_IUs);
	private static final CommandLineOption OPTION_UNINSTALL_IU = new CommandLineOption(new String[] { //
			"-uninstallIU", "-uninstallIUs", "-u" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_Uninstalls_the_listed_IUs);
	private static final CommandLineOption OPTION_REVERT = new CommandLineOption(new String[] { //
			"-revert" }, //$NON-NLS-1$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_Revert_to_previous_state);
	private static final CommandLineOption OPTION_DESTINATION = new CommandLineOption(new String[] { //
			"-destination", "-d" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_path_gt, Messages.Help_The_folder_in_which_the_targetd_product_is_located);
	private static final CommandLineOption OPTION_METADATAREPOS = new CommandLineOption(new String[] { //
			"-metadatarepository", "metadatarepositories", "-m" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_A_list_of_URLs_denoting_metadata_repositories);
	private static final CommandLineOption OPTION_ARTIFACTREPOS = new CommandLineOption(new String[] { //
			"-artifactrepository", "artifactrepositories", "-a" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_A_list_of_URLs_denoting_artifact_repositories);
	private static final CommandLineOption OPTION_REPOSITORIES = new CommandLineOption(new String[] { //
			"-repository", "repositories", "-r" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_A_list_of_URLs_denoting_colocated_repositories);
	private static final CommandLineOption OPTION_VERIFY_ONLY = new CommandLineOption(new String[] { //
			"-verifyOnly" }, //$NON-NLS-1$
			null, Messages.Help_Only_verify_dont_install);
	private static final CommandLineOption OPTION_PROFILE = new CommandLineOption(new String[] { //
			"-profile", "-p" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_name_gt, Messages.Help_Defines_what_profile_to_use_for_the_actions);
	private static final CommandLineOption OPTION_FLAVOR = new CommandLineOption(new String[] { //
			"-flavor", "-f" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_name_gt, Messages.Help_Defines_flavor_to_use_for_created_profile);
	private static final CommandLineOption OPTION_SHARED = new CommandLineOption(new String[] { //
			"-shared", "-s" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lb_lt_path_gt_rb, Messages.Help_Use_a_shared_location_for_the_install);
	private static final CommandLineOption OPTION_BUNDLEPOOL = new CommandLineOption(new String[] { //
			"-bundlepool", "-b" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_path_gt, Messages.Help_The_location_where_the_plugins_and_features_will_be_stored);
	private static final CommandLineOption OPTION_IU_PROFILE_PROPS = new CommandLineOption(new String[] { //
			"-iuProfileproperties" }, //$NON-NLS-1$
			Messages.Help_lt_path_gt, Messages.Help_path_to_IU_profile_properties_file);
	private static final CommandLineOption OPTION_PROFILE_PROPS = new CommandLineOption(new String[] { //
			"-profileproperties" }, //$NON-NLS-1$
			Messages.Help_lt_comma_separated_list_gt, Messages.Help_A_list_of_properties_in_the_form_key_value_pairs);
	private static final CommandLineOption OPTION_ROAMING = new CommandLineOption(new String[] { //
			"-roaming" }, //$NON-NLS-1$
			null, Messages.Help_Indicates_that_the_product_can_be_moved);
	private static final CommandLineOption OPTION_P2_OS = new CommandLineOption(new String[] { //
			"-p2.os" }, //$NON-NLS-1$
			null, Messages.Help_The_OS_when_profile_is_created);
	private static final CommandLineOption OPTION_P2_WS = new CommandLineOption(new String[] { //
			"-p2.ws" }, //$NON-NLS-1$
			null, Messages.Help_The_WS_when_profile_is_created);
	private static final CommandLineOption OPTION_P2_ARCH = new CommandLineOption(new String[] { //
			"-p2.arch" }, //$NON-NLS-1$
			null, Messages.Help_The_ARCH_when_profile_is_created);
	private static final CommandLineOption OPTION_P2_NL = new CommandLineOption(new String[] { //
			"-p2.nl" }, //$NON-NLS-1$
			null, Messages.Help_The_NL_when_profile_is_created);
	private static final CommandLineOption OPTION_PURGEHISTORY = new CommandLineOption(new String[] { //
			"-purgeHistory" }, //$NON-NLS-1$
			null, Messages.Help_Purge_the_install_registry);
	private static final CommandLineOption OPTION_FOLLOW_REFERENCES = new CommandLineOption(new String[] { //
			"-followReferences" }, //$NON-NLS-1$
			null, Messages.Help_Follow_references);
	private static final CommandLineOption OPTION_TAG = new CommandLineOption(new String[] { //
			"-tag" }, //$NON-NLS-1$
			Messages.Help_lt_name_gt, Messages.Help_Defines_a_tag_for_provisioning_session);
	private static final CommandLineOption OPTION_LIST_TAGS = new CommandLineOption(new String[] { //
			"-listTags" }, //$NON-NLS-1$
			null, Messages.Help_List_Tags);
	private static final CommandLineOption OPTION_DOWNLOAD_ONLY = new CommandLineOption(new String[] { //
			"-downloadOnly" }, //$NON-NLS-1$
			null, Messages.Help_Download_Only);
	private static final CommandLineOption OPTION_VERBOSE_TRUST = new CommandLineOption(new String[] { //
			"-verboseTrust", "-vt" }, //$NON-NLS-1$ //$NON-NLS-2$
			null, "Whether to print detailed information about the content trust."); //$NON-NLS-1$
	private static final CommandLineOption OPTION_TRUST_SIGNED_CONTENT_ONLY = new CommandLineOption(new String[] { //
			"-trustSignedContentOnly", "-tsco" }, //$NON-NLS-1$ //$NON-NLS-2$
			null, Messages.DirectorApplication_Help_TrustSignedContentOnly);
	private static final CommandLineOption OPTION_TRUSTED_AUTHORITIES = new CommandLineOption(new String[] { //
			"-trustedAuthorities", "-ta" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_comma_separated_list_gt, Messages.DirectorApplication_Help_TrustedAuthorities);
	private static final CommandLineOption OPTION_TRUSTED_PGP_KEYS = new CommandLineOption(new String[] { //
			"-trustedPGPKeys", "-tk" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_comma_separated_list_gt, Messages.DirectorApplication_Help_TrustedKeys);
	private static final CommandLineOption OPTION_TRUSTED_CERTIFCATES = new CommandLineOption(new String[] { //
			"-trustedCertificates", "-tc" }, //$NON-NLS-1$ //$NON-NLS-2$
			Messages.Help_lt_comma_separated_list_gt, Messages.DirectorApplication_Help_TrustedCertificates);
	private static final CommandLineOption OPTION_IGNORED = new CommandLineOption(new String[] { //
			"-showLocation", "-eclipse.password", "-eclipse.keyring" }, //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
			null, ""); //$NON-NLS-1$

	private static final Integer EXIT_ERROR = 13;
	static private final String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$
	static private final String PROP_P2_PROFILE = "eclipse.p2.profile"; //$NON-NLS-1$
	static private final String NO_ARTIFACT_REPOSITORIES_AVAILABLE = "noArtifactRepositoriesAvailable"; //$NON-NLS-1$

	private static final String FOLLOW_ARTIFACT_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followArtifactRepositoryReferences"; //$NON-NLS-1$
	private static final String LIST_GROUPS_SHORTCUT = "Q:GROUP"; //$NON-NLS-1$
	private static final String QUERY_SEPARATOR = "Q:"; //$NON-NLS-1$
	private static final String QUERY_SEPARATOR_SMALL = "q:"; //$NON-NLS-1$

	public static final String LINE_SEPARATOR = System.lineSeparator();

	private static Set<URI> getAuthorityURIs(String spec) throws CoreException {
		Set<URI> result = new LinkedHashSet<>();
		List<URI> rawURIs = new ArrayList<>();
		getURIs(rawURIs, spec);
		for (URI uri : rawURIs) {
			// Avoid URIs like https://host/ in favor of https://host which actual works.
			List<URI> authorityChain = AuthorityChecker.getAuthorityChain(uri);
			URI mainAuthority = authorityChain.get(0);
			if (authorityChain.size() == 2) {
				if ((mainAuthority + "/").equals(authorityChain.get(1).toString())) { //$NON-NLS-1$
					result.add(mainAuthority);
					continue;
				}
			}

			// For longer authorities, ensure that it ends with a "/"
			if (uri.toString().endsWith("/")) { //$NON-NLS-1$
				result.add(uri);
			}

			result.add(URI.create(uri + "/")); //$NON-NLS-1$
		}
		return result;
	}

	private static void getURIs(List<URI> uris, String spec) throws CoreException {
		if (spec == null)
			return;
		String[] urlSpecs = StringHelper.getArrayFromString(spec, ',');
		for (String urlSpec : urlSpecs) {
			try {
				uris.add(new URI(urlSpec));
			} catch (URISyntaxException e1) {
				try {
					uris.add(URIUtil.fromString(urlSpec));
				} catch (URISyntaxException e) {
					throw new ProvisionException(NLS.bind(Messages.unable_to_parse_0_to_uri_1, urlSpec, e.getMessage()),
							e);
				}
			}
		}
	}

	private static String getRequiredArgument(String[] args, int argIdx) throws CoreException {
		if (argIdx < args.length) {
			String arg = args[argIdx];
			if (!arg.startsWith("-")) //$NON-NLS-1$
				return arg;
		}
		throw new ProvisionException(NLS.bind(Messages.option_0_requires_an_argument, args[argIdx - 1]));
	}

	private static String getOptionalArgument(String[] args, int argIdx) {
		// Look ahead to the next argument
		++argIdx;
		if (argIdx < args.length) {
			String arg = args[argIdx];
			if (!arg.startsWith("-")) //$NON-NLS-1$
				return arg;
		}
		return null;
	}

	private static void parseIUsArgument(List<IQuery<IInstallableUnit>> vnames, String arg) {
		String[] roots = StringHelper.getArrayFromString(arg, ',');
		for (String root : roots) {
			if (root.equalsIgnoreCase(LIST_GROUPS_SHORTCUT)) {
				vnames.add(new PrettyQuery<>(QueryUtil.createIUGroupQuery(), "All groups")); //$NON-NLS-1$
				continue;
			}
			if (root.startsWith(QUERY_SEPARATOR) || root.startsWith(QUERY_SEPARATOR_SMALL)) {
				String queryString = root.substring(2);
				vnames.add(new PrettyQuery<>(QueryUtil.createQuery(queryString, new Object[0]), queryString));
				continue;
			}
			IVersionedId vId = VersionedId.parse(root);
			Version v = vId.getVersion();
			IQuery<IInstallableUnit> query = new PrettyQuery<>(QueryUtil.createIUQuery(vId.getId(),
					Version.emptyVersion.equals(v) ? VersionRange.emptyRange : new VersionRange(v, true, v, true)),
					root);
			vnames.add(query);
		}
	}

	private static File processFileArgument(String arg) {
		if (arg.startsWith("file:")) //$NON-NLS-1$
			arg = arg.substring(5);

		// we create a path object here to handle ../ entries in the middle of paths
		return IPath.fromOSString(arg).toFile();
	}

	private IArtifactRepositoryManager artifactManager;
	IMetadataRepositoryManager metadataManager;

	private URI[] artifactReposForRemoval;
	private URI[] metadataReposForRemoval;

	private final List<URI> artifactRepositoryLocations = new ArrayList<>();
	private final List<URI> metadataRepositoryLocations = new ArrayList<>();
	private final List<IQuery<IInstallableUnit>> rootsToInstall = new ArrayList<>();
	private final List<IQuery<IInstallableUnit>> rootsToUninstall = new ArrayList<>();
	private final List<IQuery<IInstallableUnit>> rootsToList = new ArrayList<>();

	private File bundlePool = null;
	private File destination;
	private File sharedLocation;
	private String flavor;
	private boolean printHelpInfo = false;
	private boolean printIUList = false;
	private boolean printRootIUList = false;
	private boolean printTags = false;
	private IUListFormatter listFormat;

	private String revertToPreviousState = NOTHING_TO_REVERT_TO;
	private static String NOTHING_TO_REVERT_TO = "-1"; //$NON-NLS-1$
	private static String REVERT_TO_PREVIOUS = "0"; //$NON-NLS-1$
	private boolean verifyOnly;
	private boolean roamingProfile;
	private boolean purgeRegistry;
	private boolean followReferences;
	private boolean downloadOnly;
	private String profileId;
	private String profileProperties; // a comma-separated list of property pairs "tag=value"
	private String iuProfileProperties; // path to Properties file with IU profile properties
	private String ws;
	private String os;
	private String arch;
	private String nl;
	private String tag;
	private boolean verboseTrust;
	private boolean trustSignedContentOnly;
	private Set<URI> trustedAuthorityURIs;
	private Set<String> trustedPGPKeys;
	private Set<String> trustedCertificates;

	private IEngine engine;
	private boolean noProfileId;
	private IPlanner planner;
	private ILog log = new DefaultLog();

	private IProvisioningAgent targetAgent;
	private boolean targetAgentIsSelfAndUp;
	private boolean noArtifactRepositorySpecified;
	private AvoidTrustPromptService trustService;

	protected ProfileChangeRequest buildProvisioningRequest(IProfile profile, Collection<IInstallableUnit> installs,
			Collection<IInstallableUnit> uninstalls) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		markRoots(request, installs);
		markRoots(request, uninstalls);
		request.addAll(installs);
		request.removeAll(uninstalls);
		buildIUProfileProperties(request);
		return request;
	}

	// read the given file into a Properties object
	private Properties loadProperties(File file) {
		if (!file.exists()) {
			// log a warning and return
			log.log(new Status(WARNING, ID, NLS.bind(Messages.File_does_not_exist, file.getAbsolutePath())));
			return null;
		}
		Properties properties = new Properties();
		try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
			properties.load(input);
		} catch (IOException e) {
			log.log(new Status(ERROR, ID, NLS.bind(Messages.Problem_loading_file, file.getAbsolutePath()), e));
			return null;
		}
		return properties;
	}

	private void buildIUProfileProperties(IProfileChangeRequest request) {
		final String KEYWORD_KEY = "key"; //$NON-NLS-1$
		final String KEYWORD_VALUE = "value"; //$NON-NLS-1$
		final String KEYWORD_VERSION = "version"; //$NON-NLS-1$

		if (iuProfileProperties == null)
			return;

		// read the file into a Properties object for easier processing
		Properties properties = loadProperties(new File(iuProfileProperties));
		if (properties == null)
			return;

		// format for a line in the properties input file is
		// <id>.<keyword>.<uniqueNumber>=value
		// id is the IU id
		// keyword is either "key" or "value"
		// uniqueNumber is used to group keys and values together
		Set<String> alreadyProcessed = new HashSet<>();
		for (Object object : properties.keySet()) {
			String line = (String) object;
			int index = line.lastIndexOf('.');
			if (index == -1)
				continue;
			int num = -1;
			String id = null;
			try {
				num = Integer.parseInt(line.substring(index + 1));
				line = line.substring(0, index);
				index = line.lastIndexOf('.');
				if (index == -1)
					continue;
				// skip over the keyword
				id = line.substring(0, index);
			} catch (NumberFormatException e) {
				log.log(new Status(WARNING, ID, NLS.bind(Messages.Bad_format, line, iuProfileProperties), e));
				continue;
			} catch (IndexOutOfBoundsException e) {
				log.log(new Status(WARNING, ID, NLS.bind(Messages.Bad_format, line, iuProfileProperties), e));
				continue;
			}

			String versionLine = id + '.' + KEYWORD_VERSION + '.' + num;
			String keyLine = id + '.' + KEYWORD_KEY + '.' + num;
			String valueLine = id + '.' + KEYWORD_VALUE + '.' + num;

			if (alreadyProcessed.contains(versionLine) || alreadyProcessed.contains(keyLine)
					|| alreadyProcessed.contains(valueLine))
				continue;

			// skip over this key/value pair next time we see it
			alreadyProcessed.add(versionLine);
			alreadyProcessed.add(keyLine);
			alreadyProcessed.add(valueLine);

			Version version = Version.create((String) properties.get(versionLine)); // it is ok to have a null version
			String key = (String) properties.get(keyLine);
			String value = (String) properties.get(valueLine);

			if (key == null || value == null) {
				String message = NLS.bind(Messages.Unmatched_iu_profile_property_key_value, key + '/' + value);
				log.log(new Status(WARNING, ID, message));
				continue;
			}

			// lookup the IU - a null version matches all versions
			IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, version);
			// if we don't have a version the choose the latest.
			if (version == null)
				query = QueryUtil.createLatestQuery(query);
			IQueryResult<IInstallableUnit> qr = getInstallableUnits(null, query, null);
			if (qr.isEmpty()) {
				String msg = NLS.bind(Messages.Cannot_set_iu_profile_property_iu_does_not_exist, id + '/' + version);
				log.log(new Status(WARNING, ID, msg));
				continue;
			}
			IInstallableUnit iu = qr.iterator().next();
			request.setInstallableUnitProfileProperty(iu, key, value);
		}

	}

	private void cleanupRepositories() {
		if (artifactReposForRemoval != null && artifactManager != null) {
			for (int i = 0; i < artifactReposForRemoval.length && artifactReposForRemoval[i] != null; i++) {
				artifactManager.removeRepository(artifactReposForRemoval[i]);
			}
		}
		if (metadataReposForRemoval != null && metadataManager != null) {
			for (int i = 0; i < metadataReposForRemoval.length && metadataReposForRemoval[i] != null; i++) {
				metadataManager.removeRepository(metadataReposForRemoval[i]);
			}
		}
	}

	private IQueryResult<IInstallableUnit> collectRootIUs(IQuery<IInstallableUnit> query) {
		IProgressMonitor nullMonitor = new NullProgressMonitor();

		int top = metadataRepositoryLocations.size();
		if (top == 0)
			return getInstallableUnits(null, query, nullMonitor);

		List<IQueryable<IInstallableUnit>> locationQueryables = new ArrayList<>(top);
		for (int i = 0; i < top; i++)
			locationQueryables.add(new LocationQueryable(metadataRepositoryLocations.get(i)));
		return QueryUtil.compoundQueryable(locationQueryables).query(query, nullMonitor);
	}

	private Collection<IInstallableUnit> collectRoots(IProfile profile, List<IQuery<IInstallableUnit>> rootNames,
			boolean forInstall) throws CoreException {
		ArrayList<IInstallableUnit> allRoots = new ArrayList<>();
		for (IQuery<IInstallableUnit> rootQuery : rootNames) {
			IQueryResult<IInstallableUnit> roots = null;
			if (forInstall)
				roots = collectRootIUs(QueryUtil.createLatestQuery(rootQuery));

			if (roots == null || roots.isEmpty())
				roots = profile.query(rootQuery, new NullProgressMonitor());

			Iterator<IInstallableUnit> itor = roots.iterator();
			if (!itor.hasNext())
				throw new CoreException(new Status(ERROR, ID, NLS.bind(Messages.Missing_IU, rootQuery)));
			do {
				allRoots.add(itor.next());
			} while (itor.hasNext());
		}
		return allRoots;
	}

	private String getEnvironmentProperty() {
		HashMap<String, String> values = new HashMap<>();
		if (os != null)
			values.put("osgi.os", os); //$NON-NLS-1$
		if (nl != null)
			values.put("osgi.nl", nl); //$NON-NLS-1$
		if (ws != null)
			values.put("osgi.ws", ws); //$NON-NLS-1$
		if (arch != null)
			values.put("osgi.arch", arch); //$NON-NLS-1$
		return values.isEmpty() ? null : toString(values);
	}

	private IProfile getProfile() {
		IProfileRegistry profileRegistry = targetAgent.getService(IProfileRegistry.class);
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
			noProfileId = true;
		}
		return profileRegistry.getProfile(profileId);
	}

	private IProfile initializeProfile() throws CoreException {
		IProfile profile = getProfile();
		if (profile == null) {
			if (destination == null)
				missingArgument("destination"); //$NON-NLS-1$
			if (flavor == null)
				flavor = System.getProperty("eclipse.p2.configurationFlavor", FLAVOR_DEFAULT); //$NON-NLS-1$

			Map<String, String> props = new HashMap<>();
			props.put(IProfile.PROP_INSTALL_FOLDER, destination.toString());
			if (bundlePool == null)
				props.put(IProfile.PROP_CACHE,
						sharedLocation == null ? destination.getAbsolutePath() : sharedLocation.getAbsolutePath());
			else
				props.put(IProfile.PROP_CACHE, bundlePool.getAbsolutePath());
			if (roamingProfile)
				props.put(IProfile.PROP_ROAMING, Boolean.TRUE.toString());

			String env = getEnvironmentProperty();
			if (env != null)
				props.put(IProfile.PROP_ENVIRONMENTS, env);
			if (profileProperties != null)
				putProperties(profileProperties, props);
			profile = targetAgent.getService(IProfileRegistry.class).addProfile(profileId, props);
		}
		return profile;
	}

	private void initializeRepositories() throws CoreException {
		if (rootsToInstall.isEmpty() && revertToPreviousState == NOTHING_TO_REVERT_TO && !printIUList)
			// Not much point initializing repositories if we have nothing to install
			return;
		if (artifactRepositoryLocations == null)
			missingArgument("-artifactRepository"); //$NON-NLS-1$

		artifactManager = targetAgent.getService(IArtifactRepositoryManager.class);
		if (artifactManager == null)
			throw new ProvisionException(Messages.Application_NoManager);

		int removalIdx = 0;
		boolean anyValid = false; // do we have any valid repos or did they all fail to load?
		artifactReposForRemoval = new URI[artifactRepositoryLocations.size()];
		for (URI location : artifactRepositoryLocations) {
			try {
				if (!artifactManager.contains(location)) {
					artifactManager.loadRepository(location, null);
					artifactReposForRemoval[removalIdx++] = location;
				}
				anyValid = true;
			} catch (ProvisionException e) {
				// one of the repositories did not load
				log.log(e.getStatus());
			}
		}
		if (!anyValid)
			noArtifactRepositorySpecified = true;

		if (metadataRepositoryLocations == null)
			missingArgument("metadataRepository"); //$NON-NLS-1$

		metadataManager = targetAgent.getService(IMetadataRepositoryManager.class);
		if (metadataManager == null)
			throw new ProvisionException(Messages.Application_NoManager);

		removalIdx = 0;
		anyValid = false; // do we have any valid repos or did they all fail to load?
		int top = metadataRepositoryLocations.size();
		metadataReposForRemoval = new URI[top];
		for (int i = 0; i < top; i++) {
			URI location = metadataRepositoryLocations.get(i);
			try {
				if (!metadataManager.contains(location)) {
					metadataManager.loadRepository(location, null);
					metadataReposForRemoval[removalIdx++] = location;
				}
				anyValid = true;
			} catch (ProvisionException e) {
				// one of the repositories did not load
				log.log(e.getStatus());
			}
		}
		if (!anyValid)
			// all repositories failed to load
			throw new ProvisionException(Messages.Application_NoRepositories);

		if (!EngineActivator.EXTENDED)
			return;

		File[] extensions = EngineActivator.getExtensionsDirectories();

		for (File f : extensions) {
			metadataManager.addRepository(f.toURI());
			metadataManager.setRepositoryProperty(f.toURI(), EngineActivator.P2_FRAGMENT_PROPERTY,
					Boolean.TRUE.toString());
			metadataRepositoryLocations.add(f.toURI());
			artifactManager.addRepository(f.toURI());
			artifactManager.setRepositoryProperty(f.toURI(), EngineActivator.P2_FRAGMENT_PROPERTY,
					Boolean.TRUE.toString());
			artifactRepositoryLocations.add(f.toURI());
		}
	}

	private void adjustDestination() {
		// Detect the desire to have a bundled mac application and tweak the environment
		if (destination == null)
			return;
		if (org.eclipse.osgi.service.environment.Constants.OS_MACOSX.equals(os)
				&& destination.getName().endsWith(".app")) //$NON-NLS-1$
			destination = new File(destination, "Contents/Eclipse"); //$NON-NLS-1$
	}

	// Implement something here to position "p2 folder" correctly
	private void initializeServices() throws CoreException {
		if (targetAgent == null) {
			if (destination != null || sharedLocation != null) {
				File dataAreaFile = sharedLocation == null ? new File(destination, "p2") : sharedLocation;//$NON-NLS-1$
				targetAgent = createAgent(dataAreaFile.toURI());
				targetAgentIsSelfAndUp = false;
			} else {
				targetAgent = getDefaultAgent();
				targetAgentIsSelfAndUp = true;
			}
		}
		if (profileId == null) {
			if (destination != null) {
				File configIni = new File(destination, "configuration/config.ini"); //$NON-NLS-1$
				Properties ciProps = new Properties();
				try (InputStream in = new BufferedInputStream(new FileInputStream(configIni));) {
					ciProps.load(in);
					profileId = ciProps.getProperty(PROP_P2_PROFILE);
				} catch (IOException e) {
					// Ignore
				}
				if (profileId == null)
					profileId = destination.toString();
			}
		}
		if (profileId != null)
			targetAgent.registerService(PROP_P2_PROFILE, profileId);
		else
			targetAgent.unregisterService(PROP_P2_PROFILE, null);

		IDirector director = targetAgent.getService(IDirector.class);
		if (director == null)
			throw new ProvisionException(Messages.Missing_director);

		planner = targetAgent.getService(IPlanner.class);
		if (planner == null)
			throw new ProvisionException(Messages.Missing_planner);

		engine = targetAgent.getService(IEngine.class);
		if (engine == null)
			throw new ProvisionException(Messages.Missing_Engine);

		trustService = new AvoidTrustPromptService(verboseTrust, trustSignedContentOnly, trustedAuthorityURIs,
				trustedPGPKeys, trustedCertificates);
		targetAgent.registerService(UIServices.SERVICE_NAME, trustService);

		IProvisioningEventBus eventBus = targetAgent.getService(IProvisioningEventBus.class);
		if (eventBus == null)
			return;
		eventBus.addListener(this);

	}

	/**
	 * Called when the director application want to use the default provisioning
	 * agent
	 *
	 * @return the current default agent, never <code>null</code>
	 * @throws CoreException when fetching the agent failed
	 */
	protected IProvisioningAgent getDefaultAgent() throws CoreException {
		BundleContext context = Activator.getContext();
		final String currentAgentFiler = String.format("(%s=true)", IProvisioningAgent.SERVICE_CURRENT); //$NON-NLS-1$
		try {
			Collection<ServiceReference<IProvisioningAgent>> refs = context
					.getServiceReferences(IProvisioningAgent.class, currentAgentFiler);
			for (ServiceReference<IProvisioningAgent> serviceReference : refs) {
				IProvisioningAgent service = context.getService(serviceReference);
				if (service != null) {
					context.ungetService(serviceReference);
					return service;
				}
			}
		} catch (InvalidSyntaxException e) {
			// Can't happen the filter never changes
			throw new CoreException(Status.error("Internal error", e)); //$NON-NLS-1$
		}
		throw new CoreException(Status.error("Can't fetch the default agent")); //$NON-NLS-1$
	}

	/**
	 * Creates a new agent for the given data area
	 *
	 * @param p2DataArea the data area to create a new agent
	 * @return the new agent, never <code>null</code>
	 * @throws CoreException if creation of the agent for the given location failed
	 */
	protected IProvisioningAgent createAgent(URI p2DataArea) throws CoreException {
		BundleContext context = Activator.getContext();
		ServiceReference<IProvisioningAgentProvider> agentProviderRef = context
				.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = context.getService(agentProviderRef);

		IProvisioningAgent agent = provider.createAgent(p2DataArea);
		agent.registerService(IProvisioningAgent.INSTALLER_AGENT, provider.createAgent(null));
			context.ungetService(agentProviderRef);
		return agent;
	}


	/*
	 * See bug: https://bugs.eclipse.org/340971 Using the event bus to detect
	 * whether or not a repository was added in a touchpoint action. If it was, then
	 * (if it exists) remove it from our list of repos to remove after we complete
	 * our install.
	 */
	@Override
	public void notify(EventObject o) {
		if (!(o instanceof RepositoryEvent))
			return;
		RepositoryEvent event = (RepositoryEvent) o;
		if (RepositoryEvent.ADDED != event.getKind())
			return;

		// TODO BE CAREFUL SINCE WE ARE MODIFYING THE SELF PROFILE
		int type = event.getRepositoryType();
		URI location = event.getRepositoryLocation();
		if (IRepository.TYPE_ARTIFACT == type && artifactReposForRemoval != null) {
			for (int i = 0; i < artifactReposForRemoval.length; i++) {
				if (artifactReposForRemoval[i] != null && URIUtil.sameURI(artifactReposForRemoval[i], (location))) {
					artifactReposForRemoval[i] = null;
					break;
				}
			}
			// either found or not found. either way, we're done here
			return;
		}
		if (IRepository.TYPE_METADATA == type && metadataReposForRemoval != null) {
			for (int i = 0; i < metadataReposForRemoval.length; i++) {
				if (metadataReposForRemoval[i] != null && URIUtil.sameURI(metadataReposForRemoval[i], (location))) {
					metadataReposForRemoval[i] = null;
					break;
				}
			}
			// either found or not found. either way, we're done here
			return;
		}
	}

	private void markRoots(IProfileChangeRequest request, Collection<IInstallableUnit> roots) {
		for (IInstallableUnit root : roots) {
			request.setInstallableUnitProfileProperty(root, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}
	}

	private void missingArgument(String argumentName) throws CoreException {
		throw new ProvisionException(NLS.bind(Messages.Missing_Required_Argument, argumentName));
	}

	private void performList() throws CoreException {
		if (metadataRepositoryLocations.isEmpty())
			missingArgument("metadataRepository"); //$NON-NLS-1$

		ArrayList<IInstallableUnit> allRoots = new ArrayList<>();
		if (rootsToList.size() == 0) {
			for (IInstallableUnit element : collectRootIUs(QueryUtil.createIUAnyQuery()))
				allRoots.add(element);
		} else {
			for (IQuery<IInstallableUnit> root : rootsToList) {
				for (IInstallableUnit element : collectRootIUs(root))
					allRoots.add(element);
			}
		}

		allRoots.sort(null);

		String formattedString = listFormat.format(allRoots);
		System.out.println(formattedString);
	}

	private void performProvisioningActions() throws CoreException {
		IProfile profile = initializeProfile();
		Collection<IInstallableUnit> installs = collectRoots(profile, rootsToInstall, true);
		Collection<IInstallableUnit> uninstalls = collectRoots(profile, rootsToUninstall, false);

		// keep this result status in case there is a problem so we can report it to the
		// user
		boolean wasRoaming = Boolean.parseBoolean(profile.getProperty(IProfile.PROP_ROAMING));
		try {
			updateRoamingProperties(profile);

			ProvisioningContext context = new ProvisioningContext(targetAgent);
			context.setMetadataRepositories(metadataRepositoryLocations.stream().toArray(URI[]::new));
			context.setArtifactRepositories(artifactRepositoryLocations.stream().toArray(URI[]::new));
			context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, String.valueOf(followReferences));
			context.setProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, String.valueOf(followReferences));

			ProfileChangeRequest request = buildProvisioningRequest(profile, installs, uninstalls);
			printRequest(request);

			planAndExecute(profile, context, request);
		} finally {
			// if we were originally were set to be roaming and we changed it, change it
			// back before we return
			if (wasRoaming && !Boolean.parseBoolean(profile.getProperty(IProfile.PROP_ROAMING))) {
				setRoaming(profile);
			}
		}
	}

	private void planAndExecute(IProfile profile, ProvisioningContext context, ProfileChangeRequest request)
			throws CoreException {
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());

		IStatus operationStatus = result.getStatus();
		if (!operationStatus.isOK()) {
			throw new CoreException(operationStatus);
		}

		log.log(operationStatus);

		executePlan(context, result);
	}

	private void executePlan(ProvisioningContext context, IProvisioningPlan result) throws CoreException {
		if (verifyOnly) {
			return;
		}

		IStatus operationStatus;
		if (!downloadOnly)
			operationStatus = PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
		else
			operationStatus = PlanExecutionHelper.executePlan(result, engine,
					PhaseSetFactory.createPhaseSetIncluding(
							new String[] { PhaseSetFactory.PHASE_COLLECT, PhaseSetFactory.PHASE_CHECK_TRUST }),
					context, new NullProgressMonitor());

		switch (operationStatus.getSeverity()) {
		case OK:
			break;
		case INFO:
		case WARNING:
			log.log(operationStatus);
			break;
		// All other status codes correspond to IStatus.isOk() == false
		default:
			if (noArtifactRepositorySpecified && hasNoRepositoryFound(operationStatus)) {
				throw new ProvisionException(Messages.Application_NoRepositories);
			}
			throw new CoreException(operationStatus);
		}

		if (tag == null) {
			return;
		}

		long newState = result.getProfile().getTimestamp();
		IProfileRegistry registry = targetAgent.getService(IProfileRegistry.class);
		registry.setProfileStateProperty(result.getProfile().getProfileId(), newState, IProfile.STATE_PROP_TAG, tag);
	}

	private boolean hasNoRepositoryFound(IStatus status) {
		if (status.getException() != null
				&& NO_ARTIFACT_REPOSITORIES_AVAILABLE.equals(status.getException().getMessage()))
			return true;
		if (status.isMultiStatus()) {
			for (IStatus child : status.getChildren()) {
				if (hasNoRepositoryFound(child))
					return true;
			}
		}
		return false;
	}

	public void processArguments(String[] args) throws CoreException {
		if (args == null) {
			printHelpInfo = true;
			return;
		}

		// Set platform environment defaults
		EnvironmentInfo info = ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class);
		os = info.getOS();
		ws = info.getWS();
		nl = info.getNL();
		arch = info.getOSArch();

		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			String opt = args[i];
			if (OPTION_LIST.isOption(opt)) {
				printIUList = true;
				String optionalArgument = getOptionalArgument(args, i);
				if (optionalArgument != null) {
					parseIUsArgument(rootsToList, optionalArgument);
					i++;
				}
				continue;
			}

			if (OPTION_LIST_FORMAT.isOption(opt)) {
				String formatString = getRequiredArgument(args, ++i);
				listFormat = new IUListFormatter(formatString);
				continue;
			}

			if (OPTION_LIST_INSTALLED.isOption(opt)) {
				printRootIUList = true;
				continue;
			}

			if (OPTION_LIST_TAGS.isOption(opt)) {
				printTags = true;
				continue;
			}

			if (OPTION_DOWNLOAD_ONLY.isOption(opt)) {
				downloadOnly = true;
				continue;
			}

			if (OPTION_HELP.isOption(opt)) {
				printHelpInfo = true;
				continue;
			}

			if (OPTION_INSTALL_IU.isOption(opt)) {
				parseIUsArgument(rootsToInstall, getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_UNINSTALL_IU.isOption(opt)) {
				parseIUsArgument(rootsToUninstall, getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_REVERT.isOption(opt)) {
				String targettedState = getOptionalArgument(args, i);
				if (targettedState == null) {
					revertToPreviousState = REVERT_TO_PREVIOUS;
				} else {
					i++;
					revertToPreviousState = targettedState;
				}
				continue;

			}
			if (OPTION_PROFILE.isOption(opt)) {
				profileId = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_FLAVOR.isOption(opt)) {
				flavor = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_SHARED.isOption(opt)) {
				if (++i < args.length) {
					String nxt = args[i];
					if (nxt.startsWith("-")) //$NON-NLS-1$
						--i; // Oops, that's the next option, not an argument
					else
						sharedLocation = processFileArgument(nxt);
				}
				if (sharedLocation == null)
					// -shared without an argument means "Use default shared area"
					sharedLocation = IPath.fromOSString(System.getProperty("user.home")).append(".p2/").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			if (OPTION_DESTINATION.isOption(opt)) {
				destination = processFileArgument(getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_BUNDLEPOOL.isOption(opt)) {
				bundlePool = processFileArgument(getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_METADATAREPOS.isOption(opt)) {
				getURIs(metadataRepositoryLocations, getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_ARTIFACTREPOS.isOption(opt)) {
				getURIs(artifactRepositoryLocations, getRequiredArgument(args, ++i));
				continue;
			}

			if (OPTION_REPOSITORIES.isOption(opt)) {
				String arg = getRequiredArgument(args, ++i);
				getURIs(metadataRepositoryLocations, arg);
				getURIs(artifactRepositoryLocations, arg);
				continue;
			}

			if (OPTION_PROFILE_PROPS.isOption(opt)) {
				profileProperties = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_IU_PROFILE_PROPS.isOption(opt)) {
				iuProfileProperties = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_ROAMING.isOption(opt)) {
				roamingProfile = true;
				continue;
			}

			if (OPTION_VERIFY_ONLY.isOption(opt)) {
				verifyOnly = true;
				continue;
			}

			if (OPTION_PURGEHISTORY.isOption(opt)) {
				purgeRegistry = true;
				continue;
			}

			if (OPTION_FOLLOW_REFERENCES.isOption(opt)) {
				followReferences = true;
				continue;
			}

			if (OPTION_P2_OS.isOption(opt)) {
				os = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_P2_WS.isOption(opt)) {
				ws = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_P2_NL.isOption(opt)) {
				nl = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_P2_ARCH.isOption(opt)) {
				arch = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_TAG.isOption(opt)) {
				tag = getRequiredArgument(args, ++i);
				continue;
			}

			if (OPTION_VERBOSE_TRUST.isOption(opt)) {
				verboseTrust = true;
				continue;
			}

			if (OPTION_TRUST_SIGNED_CONTENT_ONLY.isOption(opt)) {
				trustSignedContentOnly = true;
				continue;
			}

			if (OPTION_TRUSTED_AUTHORITIES.isOption(opt)) {
				String optionalArgument = getOptionalArgument(args, i);
				if (optionalArgument != null) {
					i++;
				}
				trustedAuthorityURIs = getAuthorityURIs(optionalArgument);
				continue;
			}

			if (OPTION_TRUSTED_PGP_KEYS.isOption(opt)) {
				String optionalArgument = getOptionalArgument(args, i);
				if (optionalArgument != null) {
					i++;
				}
				trustedPGPKeys = new HashSet<>(Arrays.asList(StringHelper.getArrayFromString(optionalArgument, ',')));
				continue;
			}

			if (OPTION_TRUSTED_CERTIFCATES.isOption(opt)) {
				String optionalArgument = getOptionalArgument(args, i);
				if (optionalArgument != null) {
					i++;
				}
				trustedCertificates = new HashSet<>(
						Arrays.asList(StringHelper.getArrayFromString(optionalArgument, ',')));
				continue;
			}

			if (OPTION_IGNORED.isOption(opt)) {
				String optionalArgument = getOptionalArgument(args, i);
				if (optionalArgument != null) {
					i++;
				}
				continue;
			}

			if (opt != null && opt.length() > 0)
				throw new ProvisionException(NLS.bind(Messages.unknown_option_0, opt));
		}

		if (listFormat != null && !printIUList && !printRootIUList) {
			throw new ProvisionException(NLS.bind(Messages.ArgRequiresOtherArgs, //
					new String[] { OPTION_LIST_FORMAT.identifiers[0], OPTION_LIST.identifiers[0],
							OPTION_LIST_INSTALLED.identifiers[0] }));
		}

		else if (!printHelpInfo && !printIUList && !printRootIUList && !printTags && !purgeRegistry
				&& rootsToInstall.isEmpty() && rootsToUninstall.isEmpty()
				&& revertToPreviousState == NOTHING_TO_REVERT_TO) {
			log.printOut(Messages.Help_Missing_argument);
			printHelpInfo = true;
		}

		if (listFormat == null) {
			listFormat = new IUListFormatter("${id}=${version}"); //$NON-NLS-1$
		}
	}

	/**
	 * @param pairs      a comma separated list of tag=value pairs
	 * @param properties the collection into which the pairs are put
	 */
	private void putProperties(String pairs, Map<String, String> properties) {
		String[] propPairs = StringHelper.getArrayFromString(pairs, ',');
		for (String propPair : propPairs) {
			int eqIdx = propPair.indexOf('=');
			if (eqIdx < 0)
				continue;
			String tagKey = propPair.substring(0, eqIdx).trim();
			if (tagKey.length() == 0)
				continue;
			String tagValue = propPair.substring(eqIdx + 1).trim();
			if (tagValue.length() > 0)
				properties.put(tagKey, tagValue);
		}
	}

	private void cleanupServices() {
		// dispose agent, only if it is not already up and running
		if (targetAgent != null && !targetAgentIsSelfAndUp) {
			targetAgent.stop();
			targetAgent = null;
		}
	}

	public Object run(String[] args) {
		long time = System.currentTimeMillis();

		try {
			processArguments(args);
			if (printHelpInfo)
				performHelpInfo(false);
			else {
				adjustDestination();
				initializeServices();
				if (!(printIUList || printRootIUList || printTags)) {
					if (!canInstallInDestination()) {
						log.printOut(NLS.bind(Messages.Cant_write_in_destination, destination.getAbsolutePath()));
						return EXIT_ERROR;
					}
				}
				initializeRepositories();

				if (revertToPreviousState != NOTHING_TO_REVERT_TO) {
					revertToPreviousState();
				} else if (!(rootsToInstall.isEmpty() && rootsToUninstall.isEmpty()))
					performProvisioningActions();
				if (printIUList)
					performList();
				if (printRootIUList)
					performListInstalledRoots();
				if (printTags)
					performPrintTags();
				if (purgeRegistry)
					purgeRegistry();
				log.printOut(NLS.bind(Messages.Operation_complete, Long.valueOf(System.currentTimeMillis() - time)));
			}
			return IApplication.EXIT_OK;
		} catch (CoreException e) {
			IStatus error = e.getStatus();

			log.printOut(Messages.Operation_failed);
			printError(error, 0);

			log.log(error);

			// If the operation was canceled, and we aren't verbose printing the trust
			// checking information, then print it now so that the user has details about
			// which authorities, certificates, keys, and/or unsigned content is involved.
			if (error.getSeverity() == IStatus.CANCEL && !verboseTrust) {
				trustService.dump();
			}

			// set empty exit data to suppress error dialog from launcher
			setSystemProperty("eclipse.exitdata", ""); //$NON-NLS-1$ //$NON-NLS-2$
			return EXIT_ERROR;
		} finally {
			log.close();
			cleanupRepositories();
			cleanupServices();
		}
	}

	private void purgeRegistry() throws ProvisionException {
		if (getProfile() == null)
			return;
		IProfileRegistry registry = targetAgent.getService(IProfileRegistry.class);
		long[] allProfiles = registry.listProfileTimestamps(profileId);
		for (int i = 0; i < allProfiles.length - 1; i++) {
			registry.removeProfile(profileId, allProfiles[i]);
		}
	}

	private void revertToPreviousState() throws CoreException {
		IProfile profile = initializeProfile();
		IProfileRegistry profileRegistry = targetAgent.getService(IProfileRegistry.class);
		IProfile targetProfile = null;
		if (revertToPreviousState == REVERT_TO_PREVIOUS) {
			long[] profiles = profileRegistry.listProfileTimestamps(profile.getProfileId());
			if (profiles.length == 0)
				return;
			targetProfile = profileRegistry.getProfile(profile.getProfileId(), profiles[profiles.length - 1]);
		} else {
			targetProfile = profileRegistry.getProfile(profile.getProfileId(),
					getTimestampToRevertTo(profileRegistry, profile.getProfileId()));
		}

		if (targetProfile == null)
			throw new CoreException(new Status(ERROR, ID, Messages.Missing_profile));
		IProvisioningPlan plan = planner.getDiffPlan(profile, targetProfile, new NullProgressMonitor());

		ProvisioningContext context = new ProvisioningContext(targetAgent);
		context.setMetadataRepositories(
				metadataRepositoryLocations.toArray(new URI[metadataRepositoryLocations.size()]));
		context.setArtifactRepositories(
				artifactRepositoryLocations.toArray(new URI[artifactRepositoryLocations.size()]));
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, String.valueOf(followReferences));
		context.setProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, String.valueOf(followReferences));
		executePlan(context, plan);
	}

	private long getTimestampToRevertTo(IProfileRegistry profileRegistry, String profId) {
		long timestampToRevertTo = -1;
		try {
			// Deal with the case where the revert points to a timestamp
			timestampToRevertTo = Long.valueOf(revertToPreviousState).longValue();
		} catch (NumberFormatException e) {
			// Deal with the case where the revert points to tag
			Map<String, String> tags = profileRegistry.getProfileStateProperties(profId, IProfile.STATE_PROP_TAG);
			Set<Entry<String, String>> entries = tags.entrySet();
			for (Entry<String, String> entry : entries) {
				if (entry.getValue().equals(revertToPreviousState))
					try {
						long tmp = Long.valueOf(entry.getKey()).longValue();
						if (tmp > timestampToRevertTo)
							timestampToRevertTo = tmp;
					} catch (NumberFormatException e2) {
						// Not expected since the value is supposed to be a timestamp as per API
					}
			}
		}
		return timestampToRevertTo;
	}

	/**
	 * Sets a system property, using the EnvironmentInfo service if possible.
	 */
	private void setSystemProperty(String key, String value) {
		EnvironmentInfo env = ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class);
		if (env != null) {
			env.setProperty(key, value);
		} else {
			System.getProperties().put(key, value);
		}
	}

	IQueryResult<IInstallableUnit> getInstallableUnits(URI location, IQuery<IInstallableUnit> query,
			IProgressMonitor monitor) {
		IQueryable<IInstallableUnit> queryable = null;
		if (location == null) {
			queryable = metadataManager;
		} else {
			try {
				queryable = metadataManager.loadRepository(location, monitor);
			} catch (ProvisionException e) {
				// repository is not available - just return empty result
			}
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return Collector.emptyCollector();
	}

	private static void performHelpInfo(boolean documentation) {
		CommandLineOption[] allOptions = new CommandLineOption[] { //
				OPTION_METADATAREPOS, //
				OPTION_ARTIFACTREPOS, //
				OPTION_REPOSITORIES, //
				OPTION_INSTALL_IU, //
				OPTION_UNINSTALL_IU, //
				OPTION_REVERT, //
				OPTION_PURGEHISTORY, //
				OPTION_DESTINATION, //
				OPTION_LIST, //
				OPTION_LIST_TAGS, //
				OPTION_LIST_INSTALLED, //
				OPTION_LIST_FORMAT, //
				OPTION_PROFILE, //
				OPTION_PROFILE_PROPS, //
				OPTION_IU_PROFILE_PROPS, //
				OPTION_FLAVOR, //
				OPTION_BUNDLEPOOL, //
				OPTION_P2_OS, //
				OPTION_P2_WS, //
				OPTION_P2_ARCH, //
				OPTION_P2_NL, //
				OPTION_ROAMING, //
				OPTION_SHARED, //
				OPTION_TAG, //
				OPTION_VERIFY_ONLY, //
				OPTION_DOWNLOAD_ONLY, //
				OPTION_FOLLOW_REFERENCES, //
				OPTION_VERBOSE_TRUST, //
				OPTION_TRUST_SIGNED_CONTENT_ONLY, //
				OPTION_TRUSTED_AUTHORITIES, //
				OPTION_TRUSTED_PGP_KEYS, //
				OPTION_TRUSTED_CERTIFCATES, //
				OPTION_HELP, //
		};

		for (CommandLineOption allOption : allOptions) {
			if (documentation) {
				allOption.appendHelpDocumentation(System.out);
			} else {
				allOption.appendHelp(System.out);
			}
		}
	}

	/*
	 * Set the roaming property on the given profile.
	 */
	private IStatus setRoaming(IProfile profile) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.setProfileProperty(IProfile.PROP_ROAMING, "true"); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(targetAgent);
		context.setMetadataRepositories(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		return PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
	}

	private String toString(Map<String, String> context) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, String> entry : context.entrySet()) {
			if (result.length() > 0)
				result.append(',');
			result.append(entry.getKey());
			result.append('=');
			result.append(entry.getValue());
		}
		return result.toString();
	}

	private void updateRoamingProperties(IProfile profile) throws CoreException {
		// if the user didn't specify a destination path on the command-line
		// then we assume they are installing into the currently running
		// instance and we don't have anything to update
		if (destination == null)
			return;

		// if the user didn't set a profile id on the command-line this is ok if they
		// also didn't set the destination path. (handled in the case above) otherwise
		// throw an error.
		if (noProfileId) // && destination != null
			throw new ProvisionException(Messages.Missing_profileid);

		// make sure that we are set to be roaming before we update the values
		if (!Boolean.parseBoolean(profile.getProperty(IProfile.PROP_ROAMING)))
			return;

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		if (!destination.equals(new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER))))
			request.setProfileProperty(IProfile.PROP_INSTALL_FOLDER, destination.getAbsolutePath());

		File cacheLocation = null;
		if (bundlePool == null)
			cacheLocation = sharedLocation == null ? destination.getAbsoluteFile() : sharedLocation.getAbsoluteFile();
		else
			cacheLocation = bundlePool.getAbsoluteFile();
		if (!cacheLocation.equals(new File(profile.getProperty(IProfile.PROP_CACHE))))
			request.setProfileProperty(IProfile.PROP_CACHE, cacheLocation.getAbsolutePath());
		if (request.getProfileProperties().size() == 0)
			return;

		// otherwise we have to make a change so set the profile to be non-roaming so
		// the
		// values don't get recalculated to the wrong thing if we are flushed from
		// memory - we
		// will set it back later (see bug 269468)
		request.setProfileProperty(IProfile.PROP_ROAMING, "false"); //$NON-NLS-1$

		ProvisioningContext context = new ProvisioningContext(targetAgent);
		context.setMetadataRepositories(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		IStatus status = PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
		if (!status.isOK())
			throw new CoreException(new MultiStatus(ID, ERROR, new IStatus[] { status },
					NLS.bind(Messages.Cant_change_roaming, profile.getProfileId()), null));
	}

	@Override
	public void stop() {
		IProvisioningEventBus eventBus = targetAgent.getService(IProvisioningEventBus.class);
		if (eventBus != null) {
			eventBus.removeListener(this);
		}

		if (log != null) {
			log.close();
		}
	}

	public void setLog(ILog log) {
		this.log = log;
	}

	private void performListInstalledRoots() throws CoreException {
		IProfile profile = initializeProfile();
		IQueryResult<IInstallableUnit> roots = profile.query(new UserVisibleRootQuery(), null);
		Set<IInstallableUnit> sorted = new TreeSet<>(roots.toUnmodifiableSet());
		for (IInstallableUnit iu : sorted)
			System.out.println(iu.getId() + '/' + iu.getVersion());
	}

	private void performPrintTags() throws CoreException {
		IProfile profile = initializeProfile();
		IProfileRegistry registry = targetAgent.getService(IProfileRegistry.class);
		Map<String, String> tags = registry.getProfileStateProperties(profile.getProfileId(), IProfile.STATE_PROP_TAG);
		// Sort the tags from the most recent to the oldest
		List<String> timeStamps = new ArrayList<>(tags.keySet());
		timeStamps.sort(Collections.reverseOrder());
		for (String timestamp : timeStamps) {
			System.out.println(tags.get(timestamp));
		}
	}

	private void printRequest(IProfileChangeRequest request) {
		Collection<IInstallableUnit> toAdd = request.getAdditions();
		for (IInstallableUnit added : toAdd) {
			log.printOut(NLS.bind(Messages.Installing, added.getId(), added.getVersion()));
		}

		Collection<IInstallableUnit> toRemove = request.getRemovals();
		for (IInstallableUnit removed : toRemove) {
			log.printOut(NLS.bind(Messages.Uninstalling, removed.getId(), removed.getVersion()));
		}
	}

	private void printError(IStatus status, int level) {
		String prefix = emptyString(level);

		String msg = status.getMessage();
		log.printErr(prefix + msg);

		Throwable cause = status.getException();
		if (cause != null) {
			// TODO This is very unreliable. It assumes that if the IStatus message is the
			// same as the IStatus cause
			// message the cause exception has no more data to offer. Better to just print
			// it.
			boolean isCauseMsg = msg.equals(cause.getMessage()) || msg.equals(cause.toString());
			if (!isCauseMsg) {
				log.printErr(prefix + "Caused by: "); //$NON-NLS-1$
				printError(cause, level);
			}
		}

		for (IStatus child : status.getChildren()) {
			printError(child, level + 1);
		}
	}

	private void printError(Throwable trace, int level) {
		if (trace instanceof CoreException) {
			printError(((CoreException) trace).getStatus(), level);
		} else {
			String prefix = emptyString(level);

			log.printErr(prefix + trace.toString());

			Throwable cause = trace.getCause();
			if (cause != null) {
				log.printErr(prefix + "Caused by: "); //$NON-NLS-1$
				printError(cause, level);
			}
		}
	}

	private static String emptyString(int size) {
		return IntStream.range(0, size).mapToObj(i -> "\t").collect(Collectors.joining()); //$NON-NLS-1$
	}

	private boolean canInstallInDestination() throws CoreException {
		// When we are provisioning what we are running. We can always install.
		if (targetAgentIsSelfAndUp)
			return true;
		if (destination == null)
			missingArgument("destination"); //$NON-NLS-1$
		return canWrite(destination);
	}

	private static boolean canWrite(File installDir) {
		installDir.mkdirs(); // Force create the folders because otherwise the call to canWrite fails on Mac
		return installDir.isDirectory() && Files.isWritable(installDir.toPath());
	}

//	@SuppressWarnings("nls")
//	public static void main(String[] args) {
//		System.out.println(
//				"<!-- This is generated from org.eclipse.equinox.internal.p2.director.app.DirectorApplication.main(String[]) -->");
//		System.out.println("<dl>");
//		System.out.println("<dt>");
//		System.out.println("-application org.eclipse.equinox.p2.director");
//		System.out.println("</dt>");
//		System.out.println("<dd>");
//		System.out.println("The application ID.");
//		System.out.println("</dd>");
//		performHelpInfo(true);
//		System.out.println("</dl>");
//	}
}
