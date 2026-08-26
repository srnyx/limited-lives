package xyz.srnyx.limitedlives.messages;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import eu.okaeri.configs.annotation.IncludePosition;
import eu.okaeri.validator.annotation.NotNull;
import xyz.srnyx.annoyingapi.file.okaeri.SubConfig;
import xyz.srnyx.annoyingapi.message.AnnoyingMessages;
import xyz.srnyx.annoyingapi.message.json.message.JsonChatMessage;
import xyz.srnyx.limitedlives.LimitedLives;


@Include(value = AnnoyingMessages.class, position = IncludePosition.BEFORE)
public class LimitedMessages extends AnnoyingMessages {
    public LimitedMessages(@org.jetbrains.annotations.NotNull LimitedLives plugin) {
        super(plugin);
    }

    @Comment
    @NotNull public JsonChatMessage reload = defaultMessage("%prefix%Successfully reloaded the plugin@@%p%%command%@@%command%");

    @Comment
    @Comment("Placeholders: %feature%, %world%")
    @NotNull public JsonChatMessage feature_disabled = defaultMessage("%prefix%%se%%feature%%pe% is disabled in %se%%world%%pe%!");

    @Comment
    @NotNull public Lives lives = new Lives(this);

    @Comment
    @NotNull public Eat eat = new Eat(this);

    @Comment
    @NotNull public Get get = new Get(this);

    @Comment
    @NotNull public Set set = new Set(this);

    @Comment
    @NotNull public Add add = new Add(this);

    @Comment
    @NotNull public Remove remove = new Remove(this);

    @Comment
    @NotNull public Give give = new Give(this);

    @Comment
    @NotNull public Withdraw withdraw = new Withdraw(this);

    @Comment
    @Comment("Placeholders: %source%, %succeeded%, %failed%")
    @NotNull public JsonChatMessage convert = defaultMessage("%prefix%%s%%succeeded%%p% players were successfully converted from %s%%source%%p% to %s%Limited Lives%p% (%s%%failed%%p% failed)@@%p%%command%@@%command%");

    public static class Lives extends SubConfig<LimitedMessages, LimitedMessages> {
        public Lives(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @NotNull public Lose lose = new Lose(this);

        @Comment
        @NotNull public JsonChatMessage zero = getRoot().defaultMessage("%p%You lost all your lives!");

        @Comment("Placeholders: %target%, %lives%")
        @NotNull public JsonChatMessage steal = getRoot().defaultMessage("%p%You stole a life from %s%%target%%p%, you now have %s%%lives%%p%!");

        @Comment("Placeholders: %remaining==time%")
        @NotNull public JsonChatMessage grace = getRoot().defaultMessage("%p%You didn't lose a life because you're in the grace period! You have %s%%remaining==s% seconds%p% left of grace");

        public static class Lose extends SubConfig<LimitedMessages, Lives> {
            public Lose(@org.jetbrains.annotations.NotNull Lives config) {
                super(config);
            }

            @Comment("Placeholders: %lives%, %killer%")
            @NotNull public JsonChatMessage player = getRoot().defaultMessage("%p%You lost a life to %s%%killer%%p%, you now have %s%%lives%%p%!");

            @Comment("Placeholders: %lives%")
            @NotNull public JsonChatMessage other = getRoot().defaultMessage("%p%You lost a life, you now have %s%%lives%%p%!");
        }
    }

    public static class Eat extends SubConfig<LimitedMessages, LimitedMessages> {
        public Eat(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %lives%")
        @NotNull public JsonChatMessage success = getRoot().defaultMessage("%p%You consumed a life, you now have %s%%lives%%p% lives!");

        @Comment
        @Comment("Placeholders: %remaining==time%")
        @NotNull public JsonChatMessage cooldown = getRoot().defaultMessage("%p%You must wait %s%%remaining==S% milliseconds%p% before consuming another life!");

        @Comment("Placeholders: %max%")
        @NotNull public JsonChatMessage max = getRoot().defaultMessage("%p%You already have the maximum amount of lives (%s%%max%%p%)!");
    }

    public static class Get extends SubConfig<LimitedMessages, LimitedMessages> {
        public Get(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %lives%")
        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%You have %s%%lives%%p% lives@@%p%%command%@@%command%");

        @Comment("Placeholders: %lives%, %target%")
        @NotNull public JsonChatMessage other = getRoot().defaultMessage("%prefix%%s%%target%%p% has %s%%lives%%p% lives@@%p%%command%@@%command%");
    }

    public static class Set extends SubConfig<LimitedMessages, LimitedMessages> {
        public Set(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %amount%, %lives%")
        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%Set your lives to %s%%lives%@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %lives%, %target%")
        @NotNull public JsonChatMessage other = getRoot().defaultMessage("%prefix%Set %s%%target%%p%'s lives to %s%%lives%@@%p%%command%@@%command%");

        @Comment
        @Comment("Placeholders: %amount%, %target%, %min%, %max%")
        @NotNull public JsonChatMessage min = getRoot().defaultMessage("%prefix%%se%%amount%%pe% is below the minimum amount of lives (%se%%min%%pe%)!@@%pe%%command%@@%command%");

        @Comment("Placeholders: %amount%, %target%, %min%, %max%")
        @NotNull public JsonChatMessage max = getRoot().defaultMessage("%prefix%%se%%amount%%pe% is above the maximum amount of lives (%se%%max%%pe%)!@@%pe%%command%@@%command%");
    }

    public static class Add extends SubConfig<LimitedMessages, LimitedMessages> {
        public Add(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %amount%, %lives%")
        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%Gave %s%%amount%%p% lives to yourself, you now have %s%%lives%@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %lives%, %target%")
        @NotNull public JsonChatMessage other = getRoot().defaultMessage("%prefix%Gave %s%%amount%%p% lives to %s%%target%%p%, they now have %s%%lives%@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %target%, %min%, %max%")
        @NotNull public JsonChatMessage max = getRoot().defaultMessage("%prefix%%pe%Adding %se%%amount%%pe% lives would cause %se%%target%%pe% to exceed the maximum amount of lives (%se%%max%%pe%)!@@%pe%%command%@@%command%");
    }

    public static class Remove extends SubConfig<LimitedMessages, LimitedMessages> {
        public Remove(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %amount%, %lives%")
        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%Removed %s%%amount%%p% lives from yourself, you now have %s%%lives%@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %lives%, %target%")
        @NotNull public JsonChatMessage other = getRoot().defaultMessage("%prefix%Removed %s%%amount%%p% lives from %s%%target%%p%, they now have %s%%lives%@@%p%%command%@@%command%");

        @Comment
        @Comment("Placeholders: %amount%, %target%, %min%, %max%")
        @NotNull public JsonChatMessage min = getRoot().defaultMessage("%prefix%%pe%Removing %se%%amount%%pe% lives would cause %se%%target%%pe% to fall below the minimum amount of lives (%se%%min%%pe%)!@@%pe%%command%@@%command%");
    }

    public static class Give extends SubConfig<LimitedMessages, LimitedMessages> {
        public Give(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %amount%, %player%, %target%, %playerlives%, %targetlives%")
        @NotNull public JsonChatMessage player = getRoot().defaultMessage("%prefix%Gave %s%%amount%%p% lives to %s%%target%%p%, you now have %s%%playerlives%%p% lives and they have %s%%targetlives%%p% lives@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %player%, %target%, %playerlives%, %targetlives%")
        @NotNull public JsonChatMessage target = getRoot().defaultMessage("%prefix%You were given %s%%amount%%p% lives by %s%%player%%p%, you now have %s%%targetlives%%p% lives and they have %s%%playerlives%%p% lives@@%p%%command%@@%command%");

        @Comment
        @NotNull public JsonChatMessage negative = getRoot().defaultMessage("%prefix%%pe%You can't give a negative amount of lives!@@%pe%%command%@@%command%");

        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%%pe%You can't give lives to yourself!@@%pe%%command%@@%command%");

        @NotNull public JsonChatMessage last_life = getRoot().defaultMessage("%prefix%%peYou can't give away your last life!@@%pe%%command%@@%command%");
    }

    public static class Withdraw extends SubConfig<LimitedMessages, LimitedMessages> {
        public Withdraw(@org.jetbrains.annotations.NotNull LimitedMessages config) {
            super(config);
        }

        @Comment("Placeholders: %amount%")
        @NotNull public JsonChatMessage self = getRoot().defaultMessage("%prefix%Withdrew %s%%amount%%p% lives@@%p%%command%@@%command%");

        @Comment("Placeholders: %amount%, %target%")
        @NotNull public JsonChatMessage other = getRoot().defaultMessage("%prefix%Withdrew %s%%amount%%p% lives from %s%%target%%p%@@%p%%command%@@%command%");

        @Comment
        @Comment("Placeholders: %amount%, %target%, %min%")
        @NotNull public JsonChatMessage min = getRoot().defaultMessage("%prefix%%pe%Withdrawing %se%%amount%%pe% would cause %se%%target%%pe% to fall below the minimum amount of lives (%se%%min%%pe%)!@@%pe%%command%@@%command%");

        @NotNull public JsonChatMessage recipe_not_set = getRoot().defaultMessage("%prefix%%pe%The recipe for withdrawing lives has not been set!@@%pe%%command%@@%command%");

        @NotNull public JsonChatMessage negative = getRoot().defaultMessage("%prefix%%pe%You can't withdraw a negative amount of lives!@@%pe%%command%@@%command%");
    }
}
