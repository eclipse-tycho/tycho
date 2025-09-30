/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.bnd.executionlistener;

import java.util.Collection;
import java.util.Comparator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

@Component
public class PrintServicesDs {

	@Reference
	public void activate(ServiceComponentRuntime componentRuntime) {
		if (Boolean.getBoolean("launch.trace")) {
			System.out.println("============ Framework Components ==================");
			Collection<ComponentDescriptionDTO> descriptionDTOs = componentRuntime.getComponentDescriptionDTOs();
			Comparator<ComponentConfigurationDTO> byComponentName = Comparator.comparing(dto -> dto.description.name,
					String.CASE_INSENSITIVE_ORDER);
			Comparator<ComponentConfigurationDTO> byComponentState = Comparator.comparingInt(dto -> dto.state);
			descriptionDTOs.stream().flatMap(dto -> componentRuntime.getComponentConfigurationDTOs(dto).stream())
					.sorted(byComponentState.thenComparing(byComponentName)).forEachOrdered(dto -> {
						if (dto.state == ComponentConfigurationDTO.FAILED_ACTIVATION) {
							System.out.println(
									toComponentState(dto.state) + " | " + dto.description.name + " | " + dto.failure);
						} else {
							System.out.println(toComponentState(dto.state) + " | " + dto.description.name);
						}
						for (int i = 0; i < dto.unsatisfiedReferences.length; i++) {
							UnsatisfiedReferenceDTO ref = dto.unsatisfiedReferences[i];
							System.out.println("\t" + ref.name + " is missing");
						}
					});
		}
	}

	private static String toComponentState(int state) {
		switch (state) {
		case ComponentConfigurationDTO.ACTIVE:
			return "ACTIVE     ";
		case ComponentConfigurationDTO.FAILED_ACTIVATION:
			return "FAILED     ";
		case ComponentConfigurationDTO.SATISFIED:
			return "SATISFIED  ";
		case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
		case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
			return "UNSATISFIED";
		default:
			return String.valueOf(state);
		}
	}
}
