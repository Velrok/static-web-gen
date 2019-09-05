<header>
# A github pull request template example

<time class="article-date" date="2019-03-26">2019-03-26</time>
</header>
<abstract>
  This post illustrates a [GitHub Pull Request template](https://help.github.com/en/articles/creating-a-pull-request-template-for-your-repository)
  I've introduced in our team.
</abstract>

## Our workflow

At uSwitch we are using GitHub to manage our software projects.
All master branches are protected, which means no one is allows to push to
master directly, instead one has to open a pull request (PR), which must be
approved by at least one other team member.
This is mainly to make sure all processing stays [GDPR](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation)
compliant and people are not accidentally changing software to process more information that it should.

We also use [Slack](https://slack.com) to organise the communication within the
team. Any PR that is opened in any of the projects generates a nicely formatted
message in our slack channel. Other team members will either review that PR or
simply read the message to keep up to date with the latest changes to the
product.

[Trello](https://trello.com) is our digital card board, which we use to organize
our work.

## An opportunity for improved communication and documentation

Since PR's have to be created and are also automatically communicated via Slack
they present an opportunity to improve communication.

I started to make sure that all my PR's would explain the context of a change in
the first paragraph and the intended change in what follows.
This received good feedback from fellow developers as well as product
owners, who started to use the generated slack messages to quickly summarize
progress over the last month.

Since then we started to use GitHub PR templates to introduce a bit more
convenience, consistency and project specific elements to most of our pull requests.
The templates are used as a guide, to be adjusted as
required, but also as a checklist &ndash; if applicable.

## Example PR template

Here is an example of our current template for our main analytics product:

``` markdown
[Trello Card](https://trello.com/)

**Context**

<!---
Describe the wider context. How is it affecting users, how has the business changed for this to be relevant.
--->

**Issue**

<!---
Describe the issue at hand usuall a technical issue or a user need.
--->

**Fix / Change / Feature**

<!---
Fix: something was not behaving as intended.
Change: something should behave differently.
Feature: something is new.
--->

 
**Preview**

<!---
Image
--->
 
**Checklist**

works in IE 11

```

Because we use trello, most PR's now include a link to a trello card. A positive
side effect is that any spontaneous work like quick bug fixes do end up having a
card as well, which is otherwise easily overlooked.

The template has html comments under _Context_ and _Issues_ which are hopefully
good enough to stand for themselves.

The Fix / Change / Feature section is somewhat controversial, and potentially
over engineered. We are still experimenting with its usefulness.
One could argue that any good change description would make it obvious
what kind of change it is.

Preview is specific to this project, which has a user interface (UI) component.
Attaching a screenshot of the change is often more descriptive than any
text and also shows up in the Slack messages, possibly enticing other team
members to check it out in production.

The checklist in this case is also project specific. It reminds us to
check any UI changes are compatible with IE11, which we have to support.

People may delete skip and rename any part of this template. It is meant as a
guide rather then a mandatory structure.
For example, people my skip the _Preview_ and _IE11_ parts for changes that
are not effecting the UI.

## Conclusion

We started to use GitHub pull request templates as a optional guide for
developers in our team, to leverage it as a communication tool not just for
developers but the entire team. This is achieved through the integration with
Slack and motivated by the requirement that all changes are made via PR's by the
business at large.

This example template is one that we are using in a small, company internal, cross functional team.
Large open source projects have different aims for their pull requests and would
probably prefer a template that is more prescriptive.

We use it as a guide to encourage reflection of the change at hand, and to
improve communication within the team. It is entirely optional and should make
our lives easier.

Thank you for your interest.
