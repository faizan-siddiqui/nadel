package graphql.nadel;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.SchemaDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.language.ObjectTypeDefinition.newObjectTypeDefinition;

public class OverallSchemaGenerator {


    public GraphQLSchema buildOverallSchema(List<DefinitionRegistry> serviceRegistries) {
        //TODO: This will not work for Unions and interfaces as they require TypeResolver
        // need to loose this requirement or add dummy versions
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        return schemaGenerator.makeExecutableSchema(createTypeRegistry(serviceRegistries), runtimeWiring);
    }

    private TypeDefinitionRegistry createTypeRegistry(List<DefinitionRegistry> serviceRegistries) {
        //TODO: this merging not completely correct for example schema definition nodes are not handled correctly
        Map<Operation, List<FieldDefinition>> fieldsMapbyType = new HashMap<>();
        Arrays.stream(Operation.values()).forEach(
                value -> fieldsMapbyType.put(value, new ArrayList<>()));

        TypeDefinitionRegistry overallRegistry = new TypeDefinitionRegistry();
        List<SDLDefinition> allDefinitions = new ArrayList<>();

        for (DefinitionRegistry definitionRegistry : serviceRegistries) {
            Map<Operation, ObjectTypeDefinition> opsTypes = definitionRegistry.getOperationMap();
            opsTypes.keySet().stream().forEach(opsType -> {
                ObjectTypeDefinition opsDefinitions = opsTypes.get(opsType);
                if (opsDefinitions != null) {
                    fieldsMapbyType.get(opsType).addAll(opsDefinitions.getFieldDefinitions());
                }
                definitionRegistry
                        .getDefinitions()
                        .stream()
                        .filter(sdlDefinition -> !(sdlDefinition instanceof SchemaDefinition) && sdlDefinition != opsDefinitions)
                        .forEach(allDefinitions::add);
            });
        }

        fieldsMapbyType.keySet().stream().forEach(key -> {
            overallRegistry.add(newObjectTypeDefinition().name(key.getDisplayName()).fieldDefinitions(fieldsMapbyType.get(key)).build());
        });

        allDefinitions.forEach(overallRegistry::add);
        return overallRegistry;
    }

}
