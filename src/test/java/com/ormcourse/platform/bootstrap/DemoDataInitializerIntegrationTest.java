package com.ormcourse.platform.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.ormcourse.platform.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;

class DemoDataInitializerIntegrationTest extends IntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void demoDataIsDisabledInTestProfile() {
        assertThat(applicationContext.getBeanNamesForType(DemoDataInitializer.class)).isEmpty();
    }

    @Test
    void demoDataIsLoadedOnceAndSkippedOnNextStart() {
        DemoDataInitializer initializer = applicationContext.getAutowireCapableBeanFactory().createBean(DemoDataInitializer.class);

        initializer.run(new DefaultApplicationArguments());
        long users = countRows("users");
        long submissions = countRows("quiz_submissions");
        initializer.run(new DefaultApplicationArguments());

        assertThat(users).isEqualTo(6);
        assertThat(countRows("users")).isEqualTo(users);
        assertThat(countRows("courses")).isEqualTo(3);
        assertThat(countRows("enrollments")).isEqualTo(5);
        assertThat(countRows("quiz_submissions")).isEqualTo(submissions).isEqualTo(5);
        assertThat(countRows("notifications")).isPositive();
    }
}
