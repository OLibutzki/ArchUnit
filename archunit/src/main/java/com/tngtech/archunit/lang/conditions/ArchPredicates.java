package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.HasName;
import com.tngtech.archunit.core.HasParameters;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.TypeDetails;

import static com.tngtech.archunit.core.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.Formatters.formatMethodParameters;
import static com.tngtech.archunit.core.JavaClass.REFLECT;

public class ArchPredicates {
    private ArchPredicates() {
    }

    /**
     * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
     * characters, '..' stands for any sequence of packages.
     * For further details see {@link com.tngtech.archunit.lang.conditions.PackageMatcher}.
     *
     * @param packageIdentifier A string representing the identifier to match packages against
     * @return A {@link com.tngtech.archunit.core.DescribedPredicate} returning true iff the package of the
     * tested {@link com.tngtech.archunit.core.JavaClass} matches the identifier
     */
    public static DescribedPredicate<JavaClass> resideIn(final String packageIdentifier) {
        return new DescribedPredicate<JavaClass>(String.format("reside in '%s'", packageIdentifier)) {
            private final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

            @Override
            public boolean apply(JavaClass input) {
                return packageMatcher.matches(input.getPackage());
            }
        };
    }

    public static DescribedPredicate<JavaClass> annotatedWith(final Class<? extends Annotation> annotationType) {
        return new DescribedPredicate<JavaClass>("annotated with @" + annotationType.getSimpleName()) {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnnotatedWith(annotationType);
            }
        };
    }

    /**
     * Predicate for matching names against a regular expression.
     */
    public static DescribedPredicate<HasName> named(final String regex) {
        final Pattern pattern = Pattern.compile(regex);
        return new DescribedPredicate<HasName>(String.format("named '%s'", regex)) {
            @Override
            public boolean apply(HasName input) {
                return pattern.matcher(input.getName()).matches();
            }
        };
    }

    public static DescribedPredicate<JavaClass> theHierarchyOf(Class<?> type) {
        return theHierarchyOfAClassThat(equalTo((Class) type).onResultOf(REFLECT))
                .as("the hierarchy of %s.class", type.getSimpleName());
    }

    public static DescribedPredicate<JavaClass> theHierarchyOfAClassThat(final DescribedPredicate<? super JavaClass> predicate) {
        return new DescribedPredicate<JavaClass>("the hierarchy of a class that " + predicate.getDescription()) {
            @Override
            public boolean apply(JavaClass input) {
                JavaClass current = input;
                while (current.getSuperClass().isPresent() && !predicate.apply(current)) {
                    current = current.getSuperClass().get();
                }
                return predicate.apply(current);
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> ownerAndNameAre(final Class<?> target, final String fieldName) {
        return new DescribedPredicate<JavaFieldAccess>(
                String.format("owner is %s and name is '%s'", target.getName(), fieldName)) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return ownerIs(target).apply(input) &&
                        fieldName.equals(input.getTarget().getName());
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> ownerIs(final Class<?> target) {
        return new DescribedPredicate<JavaFieldAccess>("owner is " + target.getName()) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return input.getTarget().getOwner().reflect() == target;
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> accessType(final AccessType accessType) {
        return new DescribedPredicate<JavaFieldAccess>("access type " + accessType) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return accessType == input.getAccessType();
            }
        };
    }

    /**
     * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
     * characters, '..' stands for any sequence of packages.
     * For further details see {@link PackageMatcher}.
     *
     * @param packageIdentifier A string representing the identifier to match packages against
     * @return A {@link com.tngtech.archunit.core.DescribedPredicate} returning true iff the package of the
     * tested {@link JavaClass} matches the identifier
     */
    public static DescribedPredicate<JavaFieldAccess> targetTypeResidesIn(String packageIdentifier) {
        final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

        return new DescribedPredicate<JavaFieldAccess>("target type resides in '%s'", packageIdentifier) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                TypeDetails fieldType = input.getTarget().getType();

                return fieldType.getPackage() != null &&
                        packageMatcher.matches(fieldType.getPackage());
            }
        };
    }

    public static DescribedPredicate<HasParameters> hasParameters(final List<TypeDetails> paramTypes) {
        return new DescribedPredicate<HasParameters>("has parameters [%s]", formatMethodParameters(paramTypes)) {
            @Override
            public boolean apply(HasParameters input) {
                return paramTypes.equals(input.getParameters());
            }
        };
    }

    public static CallPredicate callTarget() {
        return CallPredicate.target();
    }

    public static CallPredicate callOrigin() {
        return CallPredicate.origin();
    }
}
