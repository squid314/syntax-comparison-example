package com.squid314.examples.collections;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.*;

public class UserService {
    /*
     * We present several use cases for comparison:
     *
     * - Find user named "John"
     * - Find all Roles of non-admin Users which have a name containing the string "foo".
     * - Find all Groups of admin Users which have a name beginning with the string "c".
     *
     * These cases will be implemented using various different libraries and syntaxes to show how the implementations differ and what gains
     * and losses one might incur by using a specific style or library.
     */

    /*
     * Java 1.5, no libraries
     *
     * Pros:
     *   - no external dependencies
     * Cons:
     *   - not very flexible
     *   - difficult to refactor
     */
    public static class Java5 {
        public static User findJohn(List<User> users) {
            for (User user : users) {
                if (user.getUsername().equals("John")) {
                    return user;
                }
            }

            /* Debate exists about what should be done here. Should you return null indicating the user was not found? Should you throw an exception
             * indicating that it was not possible to find the user? The answer may depend on whether it is considered an error for the indicated user to not
             * be present in the set passed in. */
            return null;
        }

        public static List<Role> nonAdminFooRoles(List<User> users) {
            // Generally we start by initializing a new collection to hold the result.
            ArrayList<Role> resultRoles = new ArrayList<>();

            // Loop over the users
            for (User user : users) {
                // If the user is a non-admin...
                if (!user.isAdmin()) {
                    // ...and if their role name contains the string "foo"...
                    if (user.getRole().getName().contains("foo")) {
                        // ...add the role to the result set.
                        resultRoles.add(user.getRole());
                    }
                }
            }

            // Return the result set we built.
            return resultRoles;
        }

        public static List<Group> adminCGroups(List<User> users) {
            // Initialize a collection to hold the result.
            ArrayList<Group> resultGroups = new ArrayList<Group>();

            // Loop over the users
            for (User user : users) {
                // If the user is a non-admin...
                if (user.isAdmin()) {
                    // ...then test their groups.
                    for (Group group : user.getGroups()) {
                        // If the group name starts with "c"...
                        if (group.getName().startsWith("c")) {
                            // ...add the group to the result set.
                            resultGroups.add(group);
                        }
                    }
                }
            }

            // Return the result set we built.
            return resultGroups;
        }
    }

    /*
     * Java 1.5 using Google Guava library; functional style
     *
     * Pros:
     *   - easier to read what operations are occurring, especially when using variables or methods to provide Functions and Predicates
     *   - generally easier to refactor
     *   - functional aspect allows certain logic components to be refactored and be pluggable instead of hard-coded
     * Cons:
     *   - depends on external library
     *   - not easy to read left-to-right due to static methods, especially when applying several operations
     *   - requires large amounts of boilerplate to provide the functional operations (specifically the anonymous inner classes wrapping
     *     the actual logic)
     *   - if not extracting Functions/Predicates to variables/methods, inline classes make code harder to read, rather than easier
     * Notes:
     *   - Guava generally uses lazy evaluation when applying filters/transforms to collections. This can be both good and bad in different
     *     situations. It allows all modifications of the result set to be built up before any code is actually executed. This can save a
     *     large amount of memory (and garbage collection) if there are a lot of manipulations done before the "final" form. However, there
     *     is no caching mechanism in the manipulated states. This means that if multiple iterations of the transformed collection are
     *     performed, all functions must be applied multiple times. In cases where the manipulations generate new objects, this will cause
     *     multiple copies of the (hopefully) equivalent objects to be created, meaning more memory than necessary will be allocated. This
     *     can be prevented by submitting the result of a stream of manipulations to a new data structure, commonly a List or Set:
     *     new ArrayList<String>(transform(filter(original, condition), extraction)).
     */
    public static class Guava {
        // Optional is a pattern which prevents a null from being used in normal operations (preventing a NullPointerException) and allowing it to be obvious
        // when a value "may or may not be present" and when a value "must be present or there is an error"
        public static Optional<User> findJohn(List<User> users) {
            // search the users for a match
            return Iterables.tryFind(users, withUsername("John"));
        }

        public static Iterable<Role> nonAdminFooRoles(List<User> users) {
            // This is what the operation looks like without local variables for intermediate steps
            //return filter(transform(filter(users, not(isAdmin)), toRole), contains("foo"));

            // first filter the list to non-admins
            Iterable<User> nonAdmins = filter(users, not(isAdmin));
            // extract the Role of the User objects
            Iterable<Role> roles = transform(nonAdmins, toRole);
            // filter the roles to ones matching the "foo" criteria
            return filter(roles, contains("foo"));
        }

        public static Iterable<Group> adminCGroups(List<User> users) {
            Iterable<User> admins = filter(users, isAdmin);
            Iterable<List<Group>> groupsOfAdminGroups = transform(admins, toGroups);
            Iterable<Group> adminGroups = concat(groupsOfAdminGroups);
            return filter(adminGroups, startsWith("c"));
        }

        // Utility constants and methods

        /** {@link Predicate} to test if a user is an admin. */
        private static final Predicate<? super User> isAdmin = new Predicate<User>() {
            @Override
            public boolean apply(User user) {
                return user.isAdmin();
            }
        };
        /** {@link Function} to extract the {@link User#role} from a {@link User}. */
        private static final Function<? super User, Role> toRole = new Function<User, Role>() {
            @Override
            public Role apply(User user) {
                return user.getRole();
            }
        };
        /** {@link Function} to extract the {@link User#groups} from a {@link User}. */
        private static final Function<? super User, List<Group>> toGroups = new Function<User, List<Group>>() {
            @Override
            public List<Group> apply(User user) {
                return user.getGroups();
            }
        };

        /**
         * Produces a {@link Predicate} to test if a {@link User}'s {@link User#getUsername() username} matches the provided string.
         *
         * @param name the name for which to test the {@link User#getUsername()}.
         * @return {@link Predicate} to perform the test.
         */
        private static Predicate<User> withUsername(final String name) {
            return new Predicate<User>() {
                @Override
                public boolean apply(User user) {
                    return user.getUsername().equals(name);
                }
            };
        }

        /**
         * Produces a {@link Predicate} to test if a {@link Group}'s {@link Group#name name} begins with the specified {@code prefix}.
         *
         * @param prefix the prefix for which to test the {@link Group#name}.
         * @return {@link Predicate} to perform the test.
         */
        private static Predicate<? super Group> startsWith(final String prefix) {
            return new Predicate<Group>() {
                @Override
                public boolean apply(Group group) {
                    return group.getName().startsWith(prefix);
                }
            };
        }

        /**
         * Produces a {@link Predicate} to test if a {@link Role}'s {@link Role#name name} contains the specified string.
         *
         * @param s the string for which to test the {@link Role#name}.
         * @return {@link Predicate} to perform the test.
         */
        private static Predicate<? super Role> contains(final String s) {
            return new Predicate<Role>() {
                @Override
                public boolean apply(Role role) {
                    return role.getName().contains(s);
                }
            };
        }
    }

    /*
     * Java 1.8 using the new {@link Stream}s and {@link FunctionalInterface}s
     *
     * Pros:
     *   - Stream API and Java 8 function notation allows the operations to be very easily read and understood
     *   - simple to write
     *   - easy to refactor, many points to allow configuration
     *   - manipulations are easily composable to extend manipulations as needed by consumer
     * Cons:
     *   - requires Java 8, which doesn't have much adoption yet
     *   - Stream isn't usable with older Collection-based APIs
     * Notes:
     *   - Like the Guava library, the Stream API is "laziness-seeking" and has the same benefits/drawbacks mentioned above.
     */
    public static class Java8 {
        public static java.util.Optional<User> findJohn(List<User> users) {
            // Note: lambdas require any used variables to be final; however, the compiler is able to detect if variables are "effectively final" and
            // automatically makes them final as necessary. Try replacing "name" in the lambda with "nonFinalName" and see what happens.
            String nonFinalName = "Jo";
            nonFinalName += "hn";
            String name = nonFinalName;
            return users.stream()
                    .filter(user -> user.getUsername().equals(name))
                    .findFirst();
        }

        public static Stream<Role> nonAdminFooRoles(List<User> users) {
            return users.stream()
                    .filter(user -> !user.isAdmin())
                    .map(User::getRole)
                    .filter(role -> role.getName().contains("foo"));
        }

        public static Stream<Group> adminCGroups(List<User> users) {
            return users.stream()
                    .filter(User::isAdmin)
                    .flatMap(user -> user.getGroups().stream())
                    .filter(group -> group.getName().startsWith("c"));
        }
    }
}
