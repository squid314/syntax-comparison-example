package com.squid314.examples.collections;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    /*
     * We present several use cases for comparison:
     *
     * - Find user named "John"
     * - Determine if there is an admin in the set
     * - Find all admin usernames
     * - Does the provided set of Users contain any with a Role named "bar".
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
                    // We use a return here to implement a short-circuit operation so that we don't continue searching if there is one which matches. A naive
                    // implementation might assign user to a local variable and continue examining the elements of the list. Not only would this perform a
                    // large number of tests which were unnecessary, but it would also return the last element which matched instead of the first. This may
                    // be a problem in some situations.
                    return user;
                }
            }

            /* Debate exists about what should be done here. Should you return null indicating the user was not found? Should you throw an exception
             * indicating that it was not possible to find the user? The answer may depend on whether it is considered an error for the indicated user to not
             * be present in the set passed in. */
            return null;
        }

        public static boolean anyAdmins(List<User> users) {
            for (User user : users) {
                if (user.isAdmin()) {
                    return true;
                }
            }
            return false;
        }

        public static List<String> adminUsernames(List<User> users) {
            ArrayList<String> result = new ArrayList<String>();

            for (User user : users) {
                if (user.isAdmin()) {
                    result.add(user.getUsername());
                }
            }

            return result;
        }

        public static boolean anyRoleBarUsers(List<User> users) {
            for (User user : users) {
                if (user.getRole().getName().equals("bar")) {
                    return true;
                }
            }
            return false;
        }

        public static List<Role> nonAdminFooRoles(List<User> users) {
            // Generally we start by initializing a new collection to hold the result.
            ArrayList<Role> resultRoles = new ArrayList<Role>();

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
     * Java 1.5 using Google Guava library on java.util.* items; functional style
     *
     * Pros:
     *   - easier to read what operations are occurring, especially when using variables or methods to provide Functions and Predicates
     *   - generally easier to refactor
     *   - functional aspect allows certain logic components to be refactored and be pluggable instead of hard-coded
     *   - operates directly on java.util.* Collections Framework objects making it very simple to interoperate with other libraries
     * Cons:
     *   - depends on external library
     *   - requires large amounts of boilerplate to provide the functional operations (specifically the anonymous inner classes wrapping
     *     the actual logic)
     *   - if not extracting Functions/Predicates to variables/methods, inline classes make code harder to read, rather than easier
     *   - not easy to read left-to-right due to static methods, especially when applying several operations
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
        // when a value "may or may not be present" and when a value "must be present or there is an error".
        public static Optional<User> findJohn(List<User> users) {
            // Search the users for a match. tryFind is a short-circuit operation which will stop searching as soon as a match is found.
            return Iterables.tryFind(users, withUsername("John"));
        }

        public static boolean anyAdmins(List<User> users) {
            // Optional will tell us if there is a value which matches by asking if the value "is present".
            return Iterables.tryFind(users, isAdmin).isPresent();
        }

        public static Collection<String> adminUsernames(List<User> users) {
            Collection<User> admins = Collections2.filter(users, isAdmin);
            return Collections2.transform(admins, toUsername);
        }

        public static boolean anyRoleBarUsers(List<User> users) {
            Collection<Role> roles = Collections2.transform(users, toRole);
            Collection<String> roleNames = Collections2.transform(roles, roleToName);
            // This Predicate doesn't seem particularly reusable, so we will just inline it here.
            Predicate<String> isBar = new Predicate<String>() {
                @Override
                public boolean apply(String roleName) {
                    return roleName.equals("bar");
                }
            };
            return Iterables.tryFind(roleNames, isBar).isPresent();
        }

        public static Collection<Role> nonAdminFooRoles(List<User> users) {
            // This is what the operation looks like without local variables for intermediate steps and using static method imports
            //return filter(transform(filter(users, not(isAdmin)), toRole), contains("foo"));

            // first filter the list to non-admins
            Collection<User> nonAdmins = Collections2.filter(users, Predicates.not(isAdmin));
            // extract the Role of the User objects
            Collection<Role> roles = Collections2.transform(nonAdmins, toRole);
            // filter the roles to ones matching the "foo" criteria
            return Collections2.filter(roles, contains("foo"));
        }

        public static Iterable<Group> adminCGroups(List<User> users) {
            Collection<User> admins = Collections2.filter(users, isAdmin);
            Collection<List<Group>> groupsOfAdminGroups = Collections2.transform(admins, toGroups);
            // There isn't an API for joining Collections, so we just join the Iterables instead. In most cases this will be fine.
            Iterable<Group> adminGroups = Iterables.concat(groupsOfAdminGroups);
            return Iterables.filter(adminGroups, startsWith("c"));
        }
    }

    /*
     * Java 1.5 using Google Guava library and Java 8-like classes
     *
     * Pros:
     *   - (first 3 pros of preceding Guava case)
     *   - FluentIterable makes it easier to read the steps which are being applied
     *   - easy to export from Guava types to java.util.* types (or compatible types)
     *   - provides obvious handle on the string of lazy-evaluation operations in the form of the FluentIterable; this makes it easy to
     *     notice that the current object is a not-complete state.
     * Cons:
     *   - (first 3 cons of preceding Guava case)
     *   - doesn't use java.util.* types, making it harder to interoperate with other libraries, potentially requiring many additional
     *     toList (or similar) calls
     * Notes:
     *   - As in preceding Guava case, lazy evaluation *can* cause problems. However, if the libraries you write directly return the
     *     intermediary FluentIterable (not as done here), it is obvious that the object a person is using is actually a lazy operation
     *     chain and they can then choose to act on that knowledge as they see fit.
     */
    public static class Guava_Fluent {
        public static Optional<User> findJohn(List<User> users) {
            // Wrap as a FluentIterable then search
            return FluentIterable.from(users).firstMatch(withUsername("John"));
            // This could also be written as...
            // return FluentIterable.from(users).filter(withUsername("John")).first();
        }

        public static boolean anyAdmins(List<User> users) {
            return FluentIterable.from(users).firstMatch(isAdmin).isPresent();
        }

        public static Collection<String> adminUsernames(List<User> users) {
            // Reasonably easy to read from left to right what is actually happening to build the result.
            return FluentIterable.from(users)
                    .filter(isAdmin)
                    .transform(toUsername)
                    .toList();
        }

        public static boolean anyRoleBarUsers(List<User> users) {
            Predicate<String> isBar = new Predicate<String>() {
                @Override
                public boolean apply(String roleName) {
                    return roleName.equals("bar");
                }
            };
            return FluentIterable.from(users)
                    .transform(toRole)
                    .transform(roleToName)
                    .firstMatch(isBar).isPresent();
        }

        public static Collection<Role> nonAdminFooRoles(List<User> users) {
            return FluentIterable.from(users)
                    .filter(Predicates.not(isAdmin))
                    .transform(toRole)
                    .filter(contains("foo"))
                    .toList();
        }

        public static Collection<Group> adminCGroups(List<User> users) {
            return FluentIterable.from(users)
                    .filter(isAdmin)
                    .transformAndConcat(toGroups)
                    .filter(startsWith("c"))
                    .toList();
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

        public static boolean anyAdmins(List<User> users) {
            // As with Guava, this is a short-circuit operation which stops looking as soon as it finds one which matches.
            return users.stream().anyMatch(user -> user.isAdmin());
        }

        public static Collection<String> adminUsernames(List<User> users) {
            return users.stream()
                    // In anyAdmins, we used a simple lambda to determine if the user is an admin or not. Here we use a "method reference" which does the
                    // exact same thing (they are equivalent). The difference between them is largely personal preference; I can't find best practice
                    // suggestions or documentation on whether they will generate different bytecode.
                    .filter(User::isAdmin)
                    .map(User::getUsername)
                    .collect(Collectors.toList());
        }

        public static boolean anyRoleBarUsers(List<User> users) {
            return users.stream()
                    .map(User::getRole)
                    .map(Role::getName)
                    // We can use a method of an instance we hold to create a Function or Predicate just as we can use a generic method. As long as the
                    // method can be translated into the FunctionalInterface needed. If we need a {@code Role Function#apply(User)}, we can use the 0-argument
                    // instance method User#getRole() which will use the Function#apply(User) argument as this of User#getRole(). Similarly, we can use the
                    // 1-argument instance method String#equals(Object) of an instance we control to create a Predicate.
                    .anyMatch("bar"::equals);
        }

        public static Collection<Role> nonAdminFooRoles(List<User> users) {
            return users.stream()
                    // This could also be done by casting a User::isAdmin method reference to a Predicate<User> and calling Predicate#negate() on it.
                    //.filter(((java.util.function.Predicate<User>) User::isAdmin).negate())
                    .filter(user -> !user.isAdmin())
                    .map(User::getRole)
                    .filter(role -> role.getName().contains("foo"))
                    .collect(Collectors.toList());
        }

        public static Collection<Group> adminCGroups(List<User> users) {
            return users.stream()
                    .filter(User::isAdmin)
                    .flatMap(user -> user.getGroups().stream())
                    .filter(group -> group.getName().startsWith("c"))
                    .collect(Collectors.toList());
        }
    }


    // Utility constants and methods

    /** {@link Predicate} to test if a user is an admin. */
    private static final Predicate<? super User> isAdmin = new Predicate<User>() {
        @Override
        public boolean apply(User user) {
            return user.isAdmin();
        }
    };
    /** {@link Function} to extract the {@link User#getRole()} from a {@link User}. */
    private static final Function<? super User, Role> toRole = new Function<User, Role>() {
        @Override
        public Role apply(User user) {
            return user.getRole();
        }
    };
    /** {@link Function} to extract the {@link User#getGroups()} from a {@link User}. */
    private static final Function<? super User, List<Group>> toGroups = new Function<User, List<Group>>() {
        @Override
        public List<Group> apply(User user) {
            return user.getGroups();
        }
    };
    /** {@link Function} to extract the {@link User#getUsername()} from a {@link User}. */
    private static final Function<? super User, String> toUsername = new Function<User, String>() {
        @Override
        public String apply(User user) {
            return user.getUsername();
        }
    };
    private static final Function<? super Role, String> roleToName = new Function<Role, String>() {
        @Override
        public String apply(Role role) {
            return role.getName();
        }
    };

    /**
     * Produces a {@link Predicate} to test if a {@link User}'s {@link User#getUsername() username} matches the provided string.
     *
     * @param name the name for which to test the {@link User#getUsername()}.
     * @return {@link Predicate} to perform the test.
     */
    private static Predicate<? super User> withUsername(final String name) {
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
