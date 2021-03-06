package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ExecutionPath;

import java.util.List;

@Internal
public class PathMapper {

    public ExecutionPath mapPath(ExecutionPath executionPath, String resultKey, UnapplyEnvironment environment) {
        List<Object> fieldSegments = patchLastFieldName(executionPath, resultKey);

        if (environment.isHydrationTransformation) {
            //
            // Normally the parent path is all ok and hence there is nothing to add
            // but if we have a hydrated a field then we need to "merge" the paths not just append them
            // so for example
            //
            // /issue/reporter might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
            List<Object> tmp = environment.parentNode.getExecutionPath().toList();
            tmp.add(fieldSegments.get(fieldSegments.size() - 1));
            fieldSegments = tmp;
        }
        return ExecutionPath.fromList(fieldSegments);
    }

    private List<Object> patchLastFieldName(ExecutionPath executionPath, String resultKey) {
        List<Object> fieldSegments = executionPath.toList();
        for (int i = fieldSegments.size() - 1; i >= 0; i--) {
            Object segment = fieldSegments.get(i);
            if (segment instanceof String) {
                fieldSegments.set(i, resultKey);
                break;
            }
        }
        return fieldSegments;
    }
}
