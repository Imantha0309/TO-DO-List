/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.Module
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.SerializationFeature
 *  com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 *  com.studyforge.TargetServiceTest
 *  com.studyforge.dto.TargetDtos$MemberRequest
 *  com.studyforge.dto.TargetDtos$MilestoneItem
 *  com.studyforge.dto.TargetDtos$MilestoneRequest
 *  com.studyforge.dto.TargetDtos$TargetRequest
 *  com.studyforge.dto.TargetDtos$TargetView
 *  com.studyforge.dto.TargetDtos$TaskItem
 *  com.studyforge.dto.TargetDtos$TaskRequest
 *  com.studyforge.model.Member
 *  com.studyforge.model.Milestone
 *  com.studyforge.model.Priority
 *  com.studyforge.model.Target
 *  com.studyforge.model.TargetSource
 *  com.studyforge.model.TargetStatus
 *  com.studyforge.model.Task
 *  com.studyforge.model.TaskStatus
 *  com.studyforge.model.User
 *  com.studyforge.repository.TargetRepository
 *  com.studyforge.repository.UserRepository
 *  com.studyforge.service.ProgressService
 *  com.studyforge.service.TargetService
 *  org.assertj.core.api.AbstractBooleanAssert
 *  org.assertj.core.api.Assertions
 *  org.junit.jupiter.api.BeforeEach
 *  org.junit.jupiter.api.Test
 *  org.mockito.ArgumentCaptor
 *  org.mockito.ArgumentMatchers
 *  org.mockito.Mockito
 */
package com.studyforge;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyforge.dto.TargetDtos;
import com.studyforge.model.Member;
import com.studyforge.model.Milestone;
import com.studyforge.model.Priority;
import com.studyforge.model.Target;
import com.studyforge.model.TargetSource;
import com.studyforge.model.TargetStatus;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import com.studyforge.model.User;
import com.studyforge.repository.TargetRepository;
import com.studyforge.repository.UserRepository;
import com.studyforge.service.ProgressService;
import com.studyforge.service.IdGen;
import com.studyforge.service.TargetService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class TargetServiceTest {
    private final TargetRepository repo = (TargetRepository)Mockito.mock(TargetRepository.class);
    private final UserRepository users = (UserRepository)Mockito.mock(UserRepository.class);
    private TargetService service;

    TargetServiceTest() {
    }

    @BeforeEach
    void setUp() {
        this.service = new TargetService(this.repo, new ProgressService(), this.users);
        Mockito.when(this.repo.save(ArgumentMatchers.any(Target.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TargetDtos.TargetRequest requestWithStatus(TaskStatus s1, TaskStatus s2) {
        return new TargetDtos.TargetRequest("IT3070 Group Project", "Build web app", "IT3070", LocalDate.of(2026, 9, 13), LocalDate.of(2026, 10, 20), Priority.HIGH, TargetSource.MANUAL, null, List.of(), List.of(new TargetDtos.MilestoneRequest("Kickoff", LocalDate.of(2026, 9, 25), null, List.of(new TargetDtos.TaskRequest("Write proposal", "", LocalDate.of(2026, 9, 18), Priority.MEDIUM, s1, "Member 2", List.of(), List.of()), new TargetDtos.TaskRequest("Setup repo", "", LocalDate.of(2026, 9, 20), Priority.LOW, s2, null, List.of(), List.of())))));
    }

    @Test
    void createAssignsIdsAndKeepsDates() {
        TargetDtos.TargetView view = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Assertions.assertThat((String)view.id()).isNotBlank();
        Assertions.assertThat((LocalDate)view.deadline()).isEqualTo((Object)LocalDate.of(2026, 10, 20));
        Assertions.assertThat((LocalDate)view.startDate()).isEqualTo((Object)LocalDate.of(2026, 9, 13));
        Assertions.assertThat((String)((TargetDtos.MilestoneItem)view.milestones().get(0)).id()).isNotBlank();
        Assertions.assertThat((String)((TargetDtos.TaskItem)((TargetDtos.MilestoneItem)view.milestones().get(0)).tasks().get(0)).id()).isNotBlank();
        Assertions.assertThat((Comparable)view.status()).isEqualTo((Object)TargetStatus.ACTIVE);
        Assertions.assertThat((int)view.stats().progress()).isZero();
    }

    @Test
    void allTasksDoneCompletesTargetAndSetsStationedAt() {
        TargetDtos.TargetView view = this.service.create("u1", this.requestWithStatus(TaskStatus.DONE, TaskStatus.DONE));
        Assertions.assertThat((Comparable)view.status()).isEqualTo((Object)TargetStatus.COMPLETED);
        Assertions.assertThat((int)view.stats().progress()).isEqualTo(100);
        Assertions.assertThat((String)view.id()).isNotBlank();
    }

    @Test
    void reopeningCompletedTargetReopensIt() {
        TargetDtos.TargetView view = this.service.create("u1", this.requestWithStatus(TaskStatus.DONE, TaskStatus.DONE));
        ArgumentCaptor<Target> cap = ArgumentCaptor.forClass(Target.class);
        Mockito.verify(this.repo).save(cap.capture());
        Target saved = (Target)cap.getValue();
        Assertions.assertThat((Comparable)saved.status).isEqualTo((Object)TargetStatus.COMPLETED);
        Assertions.assertThat((Instant)saved.completedAt).isNotNull();
        Mockito.when(this.repo.findById(saved.id)).thenReturn(Optional.of(saved));
        TargetDtos.TargetView reopened = this.service.update("u1", saved.id, this.requestWithStatus(TaskStatus.DONE, TaskStatus.TODO));
        Assertions.assertThat((Comparable)reopened.status()).isEqualTo((Object)TargetStatus.ACTIVE);
    }

    @Test
    void serializesDatesAsIsoInViews() throws Exception {
        TargetDtos.TargetView view = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        ObjectMapper mapper = new ObjectMapper().registerModule((Module)new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString((Object)view);
        Assertions.assertThat((String)json).contains(new CharSequence[]{"\"deadline\":\"2026-10-20\""});
        Assertions.assertThat((String)json).contains(new CharSequence[]{"\"startDate\":\"2026-09-13\""});
        Assertions.assertThat((String)json).contains(new CharSequence[]{"\"source\":\"MANUAL\""});
        Assertions.assertThat((String)json).contains(new CharSequence[]{"\"progress\":0"});
    }

    @Test
    void deadlineBeforeStartIsRejected() {
        TargetDtos.TargetRequest bad = new TargetDtos.TargetRequest("Bad", null, null, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 9, 13), Priority.MEDIUM, TargetSource.MANUAL, null, List.of(), List.of());
        Mockito.when(this.repo.save(ArgumentMatchers.any(Target.class))).thenAnswer(inv -> inv.getArgument(0));
        try {
            this.service.create("u1", bad);
            ((AbstractBooleanAssert)Assertions.assertThat((boolean)false).as("should have thrown", new Object[0])).isTrue();
        }
        catch (Exception e) {
            Assertions.assertThat((String)e.getMessage()).contains(new CharSequence[]{"Deadline"});
        }
    }

    @Test
    void collaboratorsCanReadAndEditButNotOwn() {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Target saved = new Target();
        saved.id = created.id();
        saved.ownerId = "u1";
        saved.title = "IT3070 Group Project";
        saved.source = TargetSource.MANUAL;
        saved.status = TargetStatus.ACTIVE;
        saved.milestones = List.of();
        saved.collaboratorIds = List.of("u2", "u3");
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        TargetDtos.TargetView collabView = this.service.get("u2", created.id());
        Assertions.assertThat((String)collabView.access()).isEqualTo("EDITOR");
        Assertions.assertThat((String)collabView.ownerId()).isEqualTo("u1");
        this.service.update("u2", created.id(), this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Assertions.assertThatThrownBy(() -> this.service.delete("u2", created.id())).isInstanceOf(Exception.class);
        Assertions.assertThatThrownBy(() -> this.service.addMember("u2", created.id(), new TargetDtos.MemberRequest("X", null, null, null))).isInstanceOf(Exception.class);
        Assertions.assertThatThrownBy(() -> this.service.get("u9", created.id())).hasMessageContaining("not found");
    }

    @Test
    void addCollaboratorLinksAccountMemberAndRejectsUnknownEmail() {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Target saved = new Target();
        saved.id = created.id();
        saved.ownerId = "u1";
        saved.title = "IT3070 Group Project";
        saved.source = TargetSource.MANUAL;
        saved.status = TargetStatus.ACTIVE;
        saved.milestones = List.of();
        saved.members = new ArrayList();
        saved.collaboratorIds = new ArrayList();
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        User alice = new User();
        alice.id = "u20";
        alice.name = "Alice";
        alice.email = "alice@univ.edu";
        Mockito.when((Object)this.users.findByEmail("alice@univ.edu")).thenReturn(Optional.of(alice));
        TargetDtos.TargetView view = this.service.addCollaborator("u1", created.id(), "Alice@Univ.edu");
        Assertions.assertThat((String)view.access()).isEqualTo("OWNER");
        Assertions.assertThat((List<Member>)view.members()).extracting(m -> m.userId).contains("u20");
        Assertions.assertThat((List<Member>)view.members()).extracting(m -> m.name).contains("Alice");
        Assertions.assertThat(saved.collaboratorIds).contains("u20");
        Assertions.assertThatThrownBy(() -> this.service.addCollaborator("u1", created.id(), "nobody@nowhere.io")).hasMessageContaining("No StudyForge account");
        User owner = new User();
        owner.id = "u1";
        owner.name = "Owner";
        owner.email = "u1@owner.io";
        Mockito.when((Object)this.users.findByEmail("u1@owner.io")).thenReturn(Optional.of(owner));
        Assertions.assertThatThrownBy(() -> this.service.addCollaborator("u1", created.id(), "u1@owner.io")).hasMessageContaining("That's you");
    }

    @Test
    void removeCollaboratorClearsAssignmentsAndEnforcesOwnerOnly() throws Exception {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Target saved = new Target();
        saved.id = created.id();
        saved.ownerId = "u1";
        saved.title = "IT3070 Group Project";
        saved.source = TargetSource.MANUAL;
        saved.status = TargetStatus.ACTIVE;
        saved.milestones = List.of();
        saved.collaboratorIds = new ArrayList(List.of((Object)"u2"));
        Member alice = new Member("m1", "Alice");
        alice.email = "alice@univ.edu";
        alice.userId = "u2";
        saved.members = new ArrayList(List.of((Object)alice));
        Milestone ms = new Milestone();
        ms.id = "ms1";
        Task task = new Task();
        task.id = "t1";
        task.title = "Write proposal";
        task.status = TaskStatus.TODO;
        task.assigneeName = "Alice";
        ms.tasks = List.of(task);
        saved.milestones = List.of(ms);
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        Assertions.assertThatThrownBy(() -> this.service.removeCollaborator("u3", created.id(), "u2")).isInstanceOf(Exception.class);
        this.service.removeCollaborator("u1", created.id(), "u2");
        Assertions.assertThat(saved.collaboratorIds).doesNotContain("u2");
        Assertions.assertThat(saved.members).isEmpty();
        Assertions.assertThat((String)task.assigneeName).isNull();
        saved.collaboratorIds.add("u9");
        TargetDtos.TargetView leaver = this.service.removeCollaborator("u9", created.id(), "u9");
Assertions.assertThat(saved.collaboratorIds).doesNotContain("u9");
        Assertions.assertThat(leaver.access()).isEqualTo("EDITOR");
    }

    private Target captureSavedTarget() {
        ArgumentCaptor<Target> cap = ArgumentCaptor.forClass(Target.class);
        Mockito.verify(this.repo).save(cap.capture());
        return cap.getValue();
    }

    @Test
    void newTasksDefaultToTodoAndSurviveRoundTrip() {
        TargetDtos.TargetRequest req = new TargetDtos.TargetRequest("Fresh plan", null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), Priority.MEDIUM, TargetSource.MANUAL, null, List.of(),
            List.of(new TargetDtos.MilestoneRequest("M1", LocalDate.of(2026, 9, 15), null, List.of(
                new TargetDtos.TaskRequest("New task", "", null, null, TaskStatus.TODO, null, List.of(), List.of()),
                new TargetDtos.TaskRequest("No status given", "", null, null, null, null, List.of(), List.of())))));
        TargetDtos.TargetView created = this.service.create("u1", req);
        Assertions.assertThat(created.status()).isEqualTo(TargetStatus.ACTIVE);
        Assertions.assertThat(created.stats().progress()).isEqualTo(0);
        Assertions.assertThat(((TargetDtos.MilestoneItem)created.milestones().get(0)).tasks().get(0).status()).isEqualTo(TaskStatus.TODO);
        Assertions.assertThat(((TargetDtos.MilestoneItem)created.milestones().get(0)).tasks().get(1).status()).isEqualTo(TaskStatus.TODO);
        Target saved = this.captureSavedTarget();
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        TargetDtos.TargetView updated = this.service.update("u1", created.id(), req);
        Assertions.assertThat(((TargetDtos.MilestoneItem)updated.milestones().get(0)).tasks().get(0).status()).isEqualTo(TaskStatus.TODO);
        Assertions.assertThat(((TargetDtos.MilestoneItem)updated.milestones().get(0)).tasks().get(1).status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void togglingOneTaskDoesNotFlipSiblings() {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Target saved = this.captureSavedTarget();
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        TargetDtos.TargetView updated = this.service.update("u1", created.id(), this.requestWithStatus(TaskStatus.DONE, TaskStatus.TODO));
        Assertions.assertThat(updated.status()).isEqualTo(TargetStatus.ACTIVE);
        Assertions.assertThat(((TargetDtos.MilestoneItem)updated.milestones().get(0)).tasks().get(0).status()).isEqualTo(TaskStatus.DONE);
        Assertions.assertThat(((TargetDtos.MilestoneItem)updated.milestones().get(0)).tasks().get(1).status()).isEqualTo(TaskStatus.TODO);
        Assertions.assertThat(updated.stats().progress()).isEqualTo(50);
    }

    @Test
    void updatePreservesExistingMilestoneAndTaskIds() {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        String msId = ((TargetDtos.MilestoneItem)created.milestones().get(0)).id();
        String taskId = ((TargetDtos.TaskItem)((TargetDtos.MilestoneItem)created.milestones().get(0)).tasks().get(0)).id();
        Target saved = this.captureSavedTarget();
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        TargetDtos.TargetView updated = this.service.update("u1", created.id(), this.requestWithStatus(TaskStatus.DONE, TaskStatus.TODO));
        Assertions.assertThat(((TargetDtos.MilestoneItem)updated.milestones().get(0)).id()).isEqualTo(msId);
        Assertions.assertThat(((TargetDtos.TaskItem)((TargetDtos.MilestoneItem)updated.milestones().get(0)).tasks().get(0)).id()).isEqualTo(taskId);
    }

    @Test
    void viewersGetReadOnlyAccessAndEditorsStayTracksInActivities() {
        TargetDtos.TargetView created = this.service.create("u1", this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO));
        Assertions.assertThat(created.activities()).anyMatch(a -> a.message.contains("Created this target"));
        Target saved = this.captureSavedTarget();
        saved.collaboratorIds = List.of("u2", "u3");
        Member viewer = new Member(IdGen.uid(), "Read Only");
        viewer.userId = "u2";
        viewer.role = "viewer";
        Member editor = new Member(IdGen.uid(), "Can Edit");
        editor.userId = "u3";
        editor.role = "editor";
        saved.members = List.of(viewer, editor);
        Mockito.when(this.repo.findById(created.id())).thenReturn(Optional.of(saved));
        Assertions.assertThat(this.service.get("u2", created.id()).access()).isEqualTo("VIEWER");
        Assertions.assertThat(this.service.get("u3", created.id()).access()).isEqualTo("EDITOR");
        Assertions.assertThat(this.service.exportCsv("u2", created.id())).contains("StudyForge plan export");
        Assertions.assertThatThrownBy(() -> this.service.update("u2", created.id(), this.requestWithStatus(TaskStatus.TODO, TaskStatus.TODO))).isInstanceOf(Exception.class);
    }
}

