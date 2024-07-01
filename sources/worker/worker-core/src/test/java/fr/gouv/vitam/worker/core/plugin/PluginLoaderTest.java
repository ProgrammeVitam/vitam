/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.processing.common.exception.InvocationPluginException;
import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PluginLoaderTest {

    @Test
    public void should_load_a_plugins_configuration() throws Exception {
        // Given
        PluginLoader pluginLoader = new PluginLoader();

        // When
        pluginLoader.loadConfiguration();

        // Then
        assertThat(pluginLoader.contains("EXAMPLE_PLUGIN")).isTrue();
        assertThat(pluginLoader.newInstance("EXAMPLE_PLUGIN")).isNotNull().isInstanceOf(FakePlugin.class);
    }

    @Test
    public void should_fail_if_plugin_is_missing() throws Exception {
        // Given
        PluginLoader pluginLoader = new PluginLoader("plugins-missing.json");

        // Then
        assertThatThrownBy(pluginLoader::loadConfiguration)
            .isInstanceOf(PluginNotFoundException.class)
            .hasMessageContaining(
                "could not find class: fr.gouv.vitam.worker.core.plugin.PluginLoaderTest$FakeMissingPlugin"
            );
    }

    @Test(expected = IllegalPathException.class)
    public void should_fail_if_path_traversal() throws Exception {
        // Given
        new PluginLoader("/vitam/../etc/passwd");
    }

    @Test(expected = IllegalPathException.class)
    public void should_fail_if_path_traversal_current_folder() throws Exception {
        // Given
        new PluginLoader("./plugins-fail.json");
    }

    @Test
    public void should_fail_if_plugin_cannot_be_loaded() throws Exception {
        // Given
        PluginLoader pluginLoader = new PluginLoader("plugins-fail.json");

        // When
        pluginLoader.loadConfiguration();

        // Then
        assertThatThrownBy(() -> pluginLoader.newInstance("EXAMPLE_PLUGIN_FAIL"))
            .isInstanceOf(InvocationPluginException.class)
            .hasMessageContaining("could not instance plugin with action Id: EXAMPLE_PLUGIN_FAIL");
    }

    @Test
    public void should_return_true_if_a_plugin_is_present() throws Exception {
        // Given
        PluginLoader pluginLoader = new PluginLoader();
        pluginLoader.loadConfiguration();

        // When
        boolean pluginIsPresent = pluginLoader.contains("EXAMPLE_PLUGIN");

        // Then
        assertThat(pluginIsPresent).isTrue();
    }

    @Test
    public void should_return_false_if_a_plugin_is_absent() throws Exception {
        // Given
        PluginLoader pluginLoader = new PluginLoader();
        pluginLoader.loadConfiguration();

        // When
        boolean pluginIsPresent = pluginLoader.contains("EXAMPLE_PLUGIN_MISSING");

        // Then
        assertThat(pluginIsPresent).isFalse();
    }

    private static class FakePlugin extends ActionHandler {

        public FakePlugin() {}

        @Override
        public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
            return null;
        }

        @Override
        public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
    }

    private static class FakeBadPlugin extends ActionHandler {

        public FakeBadPlugin() {
            throw new RuntimeException("unable to instance plugin");
        }

        @Override
        public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
            return null;
        }

        @Override
        public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
    }
}
