/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.extensions.commandlog.jpa.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.persistence.Entity;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberSupport;
import org.apache.isis.applib.annotation.PriorityPrecedence;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.jaxb.JavaSqlXMLGregorianCalendarMarshalling;
import org.apache.isis.applib.mixins.system.DomainChangeRecord;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandOutcomeHandler;
import org.apache.isis.applib.services.commanddto.conmap.UserDataKeys;
import org.apache.isis.applib.services.tablecol.TableColumnOrderForCollectionTypeAbstract;
import org.apache.isis.applib.util.TitleBuffer;
import org.apache.isis.commons.functional.Result;
import org.apache.isis.commons.internal.base._Strings;
import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.extensions.commandlog.jpa.IsisModuleExtCommandLogJpa;
import org.apache.isis.extensions.commandlog.model.command.CommandModel;
import org.apache.isis.extensions.commandlog.model.command.ReplayState;
import org.apache.isis.extensions.commandlog.model.util.BigDecimalUtils;
import org.apache.isis.extensions.commandlog.model.util.StringUtils;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.MapDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;

/**
 * A persistent representation of a {@link Command}.
 *
 * <p>
 *     Use cases requiring persistence including auditing, and for replay of
 *     commands for regression testing purposes.
 * </p>
 *
 * Note that this class doesn't subclass from {@link Command} ({@link Command}
 * is not an interface).
 */
/* TODO migrate to JPA
@javax.jdo.annotations.PersistenceCapable(
        identityType=IdentityType.APPLICATION,
        schema = "isisExtensionsCommandLog",
        table = "Command")
@javax.jdo.annotations.Queries( {
    @javax.jdo.annotations.Query(
            name="findByInteractionIdStr",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE interactionIdStr == :interactionIdStr "),
    @javax.jdo.annotations.Query(
            name="findByParent",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE parent == :parent "),
    @javax.jdo.annotations.Query(
            name="findCurrent",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE completedAt == null "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findCompleted",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE completedAt != null "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentByTarget",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE target == :target "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBetween",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE target == :target "
                    + "&& timestamp >= :from "
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampAfter",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE target == :target "
                    + "&& timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBefore",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE target == :target "
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTarget",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE target == :target "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBetween",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE timestamp >= :from "
                    + "&&    timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampAfter",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBefore",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="find",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentByUsername",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE username == :username "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findFirst",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE startedAt   != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findSince",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE timestamp > :timestamp "
                    + "   && startedAt != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC"),
    // most recent (replayed) command previously replicated from primary to
    // secondary.  This should always exist except for the very first times
    // (after restored the prod DB to secondary).
    @javax.jdo.annotations.Query(
            name="findMostRecentReplayed",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE (replayState == 'OK' || replayState == 'FAILED') "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,2"), // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
                                    // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    // the most recent completed command, as queried on the
    // secondary, corresponding to the last command run on primary before the
    // production database was restored to the secondary
    @javax.jdo.annotations.Query(
            name="findMostRecentCompleted",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE startedAt   != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findNotYetReplayed",
            value="SELECT "
                    + "FROM " + CommandJdo.FQCN
                    + " WHERE replayState == 'PENDING' "
                    + "ORDER BY this.timestamp ASC "
                    + "RANGE 0,10"),    // same as batch size
})
@javax.jdo.annotations.Indices({
        @javax.jdo.annotations.Index(name = "CommandJdo__startedAt__timestamp__IDX", members = { "startedAt", "timestamp" }),
        @javax.jdo.annotations.Index(name = "CommandJdo__timestamp__IDX", members = { "timestamp" }),
})
*/
@Entity
@DomainObject(
        logicalTypeName = CommandJpa.LOGICAL_TYPE_NAME,
        editing = Editing.DISABLED
)
@DomainObjectLayout(
        named = "Command",
        titleUiEvent = CommandModel.TitleUiEvent.class,
        iconUiEvent = CommandModel.IconUiEvent.class,
        cssClassUiEvent = CommandModel.CssClassUiEvent.class,
        layoutUiEvent = CommandModel.LayoutUiEvent.class
)
//@Log4j2
@NoArgsConstructor
public class CommandJpa
implements
    CommandModel,
    DomainChangeRecord {

    public final static String LOGICAL_TYPE_NAME = IsisModuleExtCommandLogJpa.NAMESPACE + ".Command";
    protected final static String FQCN = "org.apache.isis.extensions.commandlog.jpa.entities.CommandJpa";

    /**
     * Intended for use on primary system.
     *
     * @param command
     */
    public CommandJpa(final Command command) {

        setInteractionIdStr(command.getInteractionId().toString());
        setUsername(command.getUsername());
        setTimestamp(command.getTimestamp());

        setCommandDto(command.getCommandDto());
        setTarget(command.getTarget());
        setLogicalMemberIdentifier(command.getLogicalMemberIdentifier());

        setStartedAt(command.getStartedAt());
        setCompletedAt(command.getCompletedAt());

        setResult(command.getResult());
        setException(command.getException());

        setReplayState(ReplayState.UNDEFINED);
    }


    /**
     * Intended for use on secondary (replay) system.
     *
     * @param commandDto - obtained from the primary system as a representation of a command invocation
     * @param replayState - controls whether this is to be replayed
     * @param targetIndex - if the command represents a bulk action, then it is flattened out when replayed; this indicates which target to execute against.
     */
    public CommandJpa(
            final CommandDto commandDto,
            final ReplayState replayState,
            final int targetIndex) {

        setInteractionIdStr(commandDto.getInteractionId());
        setUsername(commandDto.getUser());
        setTimestamp(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimestamp()));

        setCommandDto(commandDto);
        setTarget(Bookmark.forOidDto(commandDto.getTargets().getOid().get(targetIndex)));
        setLogicalMemberIdentifier(commandDto.getMember().getLogicalMemberIdentifier());

        // the hierarchy of commands calling other commands is only available on the primary system, and is
        setParent(null);

        setStartedAt(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimings().getStartedAt()));
        setCompletedAt(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimings().getCompletedAt()));

        copyOver(commandDto, UserDataKeys.RESULT, value -> this.setResult(Bookmark.parse(value).orElse(null)));
        copyOver(commandDto, UserDataKeys.EXCEPTION, this::setException);

        setReplayState(replayState);
    }

    static void copyOver(
            final CommandDto commandDto,
            final String key, final Consumer<String> consume) {
        commandDto.getUserData().getEntry()
                .stream()
                .filter(x -> Objects.equals(x.getKey(), key))
                .map(MapDto.Entry::getValue)
                .filter(Objects::nonNull)
                .filter(x -> x.length() > 0)
                .findFirst()
                .ifPresent(consume);
    }

    @Service
    public static class TitleProvider {

        @EventListener(TitleUiEvent.class)
        public void on(final TitleUiEvent ev) {
            if(!Objects.equals(ev.getTitle(), "Command Jdo") || ev.getTranslatableTitle() != null) {
                return;
            }
            ev.setTitle(title((CommandJpa)ev.getSource()));
        }

        private static String title(final CommandJpa source) {
            // nb: not thread-safe
            // formats defined in https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
            val format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            val buf = new TitleBuffer();
            buf.append(format.format(source.getTimestamp()));
            buf.append(" ").append(source.getLogicalMemberIdentifier());
            return buf.toString();
        }
    }


    public static class InteractionIdDomainEvent extends PropertyDomainEvent<String> { }
    /**
     * Implementation note: persisted as a string rather than a UUID as fails
     * to persist if using h2 (perhaps would need to be mapped differently).
     * @see <a href="https://www.datanucleus.org/products/accessplatform/jdo/mapping.html#_other_types">www.datanucleus.org</a>
     */
//    @javax.jdo.annotations.PrimaryKey
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="false", name = "interactionId", length = 36)
    @Property(domainEvent = InteractionIdDomainEvent.class)
    @PropertyLayout(named = "Interaction Id")
    @Getter @Setter
    private String interactionIdStr;
    @Override
    @Programmatic
    public UUID getInteractionId() {return UUID.fromString(getInteractionIdStr());}


    public static class UsernameDomainEvent extends PropertyDomainEvent<String> { }
//    @javax.jdo.annotations.Column(allowsNull="false", length = 50)
    @Property(domainEvent = UsernameDomainEvent.class)
    @Getter @Setter
    private String username;


    public static class TimestampDomainEvent extends PropertyDomainEvent<Timestamp> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="false")
    @Property(domainEvent = TimestampDomainEvent.class)
    @Getter @Setter
    private Timestamp timestamp;



    @Override
    public ChangeType getType() {
        return ChangeType.COMMAND;
    }


    public static class ReplayStateDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a replayed command, what the outcome was.
     */
//    @javax.jdo.annotations.Column(allowsNull="true", length=10)
    @Property(domainEvent = ReplayStateDomainEvent.class)
    @Getter @Setter
    private ReplayState replayState;


    public static class ReplayStateFailureReasonDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a {@link ReplayState#FAILED failed} replayed command, what the reason was for the failure.
     */
//    @javax.jdo.annotations.Column(allowsNull="true", length=255)
    @Property(domainEvent = ReplayStateFailureReasonDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 5)
    @Getter @Setter
    private String replayStateFailureReason;
    @MemberSupport public boolean hideReplayStateFailureReason() {
        return getReplayState() == null || !getReplayState().isFailed();
    }


    public static class ParentDomainEvent extends PropertyDomainEvent<Command> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(name="parentId", allowsNull="true")
    @Property(domainEvent = ParentDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
    @Getter @Setter
    private CommandJpa parent;


    public static class TargetDomainEvent extends PropertyDomainEvent<String> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="target")
    @Property(domainEvent = TargetDomainEvent.class)
    @PropertyLayout(named = "Object")
    @Getter @Setter
    private Bookmark target;

    public String getTargetStr() {
        return Optional.ofNullable(getTarget()).map(Bookmark::toString).orElse(null);
    }

    @Override
    public String getTargetMember() {
        return getCommandDto().getMember().getLogicalMemberIdentifier();
    }

    @Property(domainEvent = TargetDomainEvent.class)
    @PropertyLayout(named = "Member")
    public String getLocalMember() {
        val targetMember = getTargetMember();
        return targetMember.substring(targetMember.indexOf("#") + 1);
    }

    public static class LogicalMemberIdentifierDomainEvent extends PropertyDomainEvent<String> { }
    @Property(domainEvent = LogicalMemberIdentifierDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
//    @javax.jdo.annotations.Column(allowsNull="false", length = MemberIdentifierType.Meta.MAX_LEN)
    @Getter @Setter
    private String logicalMemberIdentifier;


    public static class CommandDtoDomainEvent extends PropertyDomainEvent<CommandDto> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    @Property(domainEvent = CommandDtoDomainEvent.class)
    @PropertyLayout(multiLine = 9)
    @Getter @Setter
    private CommandDto commandDto;


    public static class StartedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="true")
    @Property(domainEvent = StartedAtDomainEvent.class)
    @Getter @Setter
    private Timestamp startedAt;


    public static class CompletedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="true")
    @Property(domainEvent = CompletedAtDomainEvent.class)
    @Getter @Setter
    private Timestamp completedAt;


    public static class DurationDomainEvent extends PropertyDomainEvent<BigDecimal> { }
    /**
     * The number of seconds (to 3 decimal places) that this interaction lasted.
     *
     * <p>
     * Populated only if it has {@link #getCompletedAt() completed}.
     */
//    @javax.jdo.annotations.NotPersistent
    @javax.validation.constraints.Digits(integer=5, fraction=3)
    @Property(domainEvent = DurationDomainEvent.class)
    public BigDecimal getDuration() {
        return BigDecimalUtils.durationBetween(getStartedAt(), getCompletedAt());
    }


    public static class IsCompleteDomainEvent extends PropertyDomainEvent<Boolean> { }
//    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = IsCompleteDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS)
    public boolean isComplete() {
        return getCompletedAt() != null;
    }


    public static class ResultSummaryDomainEvent extends PropertyDomainEvent<String> { }
//    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = ResultSummaryDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS, named = "Result")
    public String getResultSummary() {
        if(getCompletedAt() == null) {
            return "";
        }
        if(!_Strings.isNullOrEmpty(getException())) {
            return "EXCEPTION";
        }
        if(getResult() != null) {
            return "OK";
        } else {
            return "OK (VOID)";
        }
    }


    public static class ResultDomainEvent extends PropertyDomainEvent<String> { }
//    @javax.jdo.annotations.Persistent
//    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="result")
    @Property(domainEvent = ResultDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, named = "Result Bookmark")
    @Getter @Setter
    private Bookmark result;

    public static class ExceptionDomainEvent extends PropertyDomainEvent<String> { }
    /**
     * Stack trace of any exception that might have occurred if this interaction/transaction aborted.
     *
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so there's no object that can be accessed to be annotated.
     */
//    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    @Property(domainEvent = ExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 5, named = "Exception (if any)")
    @Getter
    private String exception;
    public void setException(final String exception) {
        this.exception = exception;
    }
    public void setException(final Throwable exception) {
        setException(_Exceptions.asStacktrace(exception));
    }

    public static class IsCausedExceptionDomainEvent extends PropertyDomainEvent<Boolean> { }
//    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = IsCausedExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS)
    public boolean isCausedException() {
        return getException() != null;
    }


    @Override
    public String getPreValue() {
        return null;
    }

    @Override
    public String getPostValue() {
        return null;
    }


    @Override
    public void saveAnalysis(final String analysis) {
        if (analysis == null) {
            setReplayState(ReplayState.OK);
        } else {
            setReplayState(ReplayState.FAILED);
            setReplayStateFailureReason(StringUtils.trimmed(analysis, 255));
        }

    }

    @Override
    public String toString() {
        return toFriendlyString();
    }

    @Override
    public CommandOutcomeHandler outcomeHandler() {
        return new CommandOutcomeHandler() {
            @Override
            public Timestamp getStartedAt() {
                return CommandJpa.this.getStartedAt();
            }

            @Override
            public void setStartedAt(final Timestamp startedAt) {
                CommandJpa.this.setStartedAt(startedAt);
            }

            @Override
            public void setCompletedAt(final Timestamp completedAt) {
                CommandJpa.this.setCompletedAt(completedAt);
            }

            @Override
            public void setResult(final Result<Bookmark> resultBookmark) {
                CommandJpa.this.setResult(resultBookmark.getValue().orElse(null));
                CommandJpa.this.setException(resultBookmark.getFailure().orElse(null));
            }

        };
    }

    @Service
    @javax.annotation.Priority(PriorityPrecedence.LATE - 10) // before the framework's own default.
    public static class TableColumnOrderDefault extends TableColumnOrderForCollectionTypeAbstract<CommandJpa> {

        public TableColumnOrderDefault() { super(CommandJpa.class); }

        @Override
        protected List<String> orderParented(final Object parent, final String collectionId, final List<String> propertyIds) {
            return ordered(propertyIds);
        }

        @Override
        protected List<String> orderStandalone(final List<String> propertyIds) {
            return ordered(propertyIds);
        }

        private List<String> ordered(final List<String> propertyIds) {
            return Arrays.asList(
                "timestamp", "target", "targetMember", "username", "complete", "resultSummary", "interactionIdStr"
            );
        }
    }
}

